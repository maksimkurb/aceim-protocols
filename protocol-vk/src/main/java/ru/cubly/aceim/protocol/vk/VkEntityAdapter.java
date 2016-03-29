package ru.cubly.aceim.protocol.vk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import ru.cubly.aceim.api.dataentity.Buddy;
import ru.cubly.aceim.api.dataentity.BuddyGroup;
import ru.cubly.aceim.api.dataentity.Message;
import ru.cubly.aceim.api.dataentity.MultiChatRoom;
import ru.cubly.aceim.api.dataentity.OnlineInfo;
import ru.cubly.aceim.api.dataentity.PersonalInfo;
import ru.cubly.aceim.api.dataentity.TextMessage;
import ru.cubly.aceim.api.service.ApiConstants;
import ru.cubly.aceim.protocol.vk.model.VkBuddy;
import ru.cubly.aceim.protocol.vk.model.VkBuddyGroup;
import ru.cubly.aceim.protocol.vk.model.VkChat;
import ru.cubly.aceim.protocol.vk.model.VkMessage;
import ru.cubly.aceim.protocol.vk.model.VkMessageAttachment;
import ru.cubly.aceim.protocol.vk.model.VkOnlineInfo;
import android.os.Bundle;
import android.text.TextUtils;

public final class VkEntityAdapter {

	private VkEntityAdapter(){}

	public static List<NameValuePair> map2NameValuePairs(Map<String, String> params) {
		if (params == null) return null;
		
		List<NameValuePair> pairs = new ArrayList<NameValuePair>(params.size());
		
		for (String key : params.keySet()) {
			pairs.add(new BasicNameValuePair(key, params.get(key)));			
		}
		
		return pairs;
	}

	public static List<BuddyGroup> vkBuddiesAndGroups2BuddyList(List<VkBuddy> buddies, List<VkBuddyGroup> groups, List<VkOnlineInfo> onlineInfos, long myId, String ownerUid, Byte serviceId) {
		if (buddies == null) {
			return null;
		}
		
		Map<String, BuddyGroup> result = new HashMap<String,BuddyGroup>(groups.size() + 1);
		BuddyGroup noGroup = new BuddyGroup(ApiConstants.NO_GROUP_ID, ownerUid, serviceId);
		
		for (VkBuddyGroup vkb : groups) {
			BuddyGroup bg = vkBuddyGroup2BuddyGroup(vkb, ownerUid, serviceId);
			result.put(bg.getId(), bg);
		}
		
		for (VkBuddy vkb : buddies) {
			Buddy b = vkBuddy2Buddy(vkb, myId, ownerUid, serviceId);
			
			for (VkOnlineInfo vki : onlineInfos) {
				if (vki.getUid() == vkb.getUid()) {
					b.getOnlineInfo().getFeatures().putByte(ApiConstants.FEATURE_STATUS, vki.getStatus());
					break;
				}
			}
			
			if (b.getGroupId().equals(ApiConstants.NO_GROUP_ID)) {
				noGroup.getBuddyList().add(b);
			} else {
				result.get(b.getGroupId()).getBuddyList().add(b);
			}
		}
		
		if (noGroup.getBuddyList().size() > 0) {
			result.put(ApiConstants.NO_GROUP_ID, noGroup);
		}
		
		return Collections.unmodifiableList(new ArrayList<BuddyGroup>(result.values()));
	}
	
	public static List<OnlineInfo> vkOnlineInfos2OnlineInfos(List<VkOnlineInfo> vkOnlineInfos, long myId, String ownerUid, Byte serviceId) {
		if (vkOnlineInfos == null) return null;
		
		List<OnlineInfo> infos = new ArrayList<OnlineInfo>(vkOnlineInfos.size());
		for (VkOnlineInfo vko : vkOnlineInfos) {
			OnlineInfo info = new OnlineInfo(serviceId, vkUid2ProtocolUid(vko.getUid(), myId, ownerUid));
			info.getFeatures().putByte(ApiConstants.FEATURE_STATUS, (byte) 0);
			infos.add(info);
		}
		
		return infos;
	}

	public static Buddy vkBuddy2Buddy(VkBuddy vkb, long myId, String ownerUid, Byte serviceId) {
		if (vkb == null) {
			return null;
		}
		
		Buddy b = new Buddy(vkUid2ProtocolUid(vkb.getUid(), myId, ownerUid), ownerUid, VkConstants.PROTOCOL_NAME, serviceId);
		
		long groupId = vkb.getGroupId();
		b.setGroupId(groupId != 0 ? Long.toString(groupId) : ApiConstants.NO_GROUP_ID);
		
		b.setName(getNickOfVkBuddy(vkb));
		
		return b;
	}

	private static String getNickOfVkBuddy(VkBuddy vkb) {
		String nick = vkb.getNickName();
		
		if (TextUtils.isEmpty(nick)) {
			String fn = vkb.getFirstName();
			String ln = vkb.getLastName();
			
			return (TextUtils.isEmpty(fn) ? "" : fn) + " " + (TextUtils.isEmpty(ln) ? "" : ln);
		} else {
			return nick;
		}
	}

	public static BuddyGroup vkBuddyGroup2BuddyGroup(VkBuddyGroup vkb, String ownerUid, Byte serviceId) {
		if (vkb == null) {
			return null;
		}
		
		BuddyGroup bg = new BuddyGroup(Long.toString(vkb.getId()), ownerUid, serviceId);
		bg.setName(vkb.getName());
		
		return bg;
	}

	public static OnlineInfo vkOnlineInfo2OnlineInfo(VkOnlineInfo vi, long myId, String ownerUid, byte serviceId) {
		if (vi == null) return null;
		
		OnlineInfo info = new OnlineInfo(serviceId, vkUid2ProtocolUid(vi.getUid(), myId, ownerUid));
		
		//Does not work either
		//info.getFeatures().putByte(ApiConstants.FEATURE_STATUS, vi.getStatus());
		info.getFeatures().putByte(ApiConstants.FEATURE_STATUS, (byte) (vi.getStatus() == 0 ? 0 : -1));
		
		return info;
	}

	public static Message vkMessage2Message(VkMessage vkm, byte serviceId, String protocolUid, long vkUid) {
		if (vkm == null) return null;
		
		//TODO support for other message types
		TextMessage tm = new TextMessage(serviceId, vkUid2ProtocolUid(vkm.getPartnerId(), vkUid, protocolUid));
		tm.setTime(vkm.getTimestamp());
		tm.setIncoming(!vkm.isOutgoing());
		tm.setMessageId(vkm.getMessageId());
		tm.setText(vkm.getText());
		
		for (VkMessageAttachment attachment : vkm.getAttachments()) {
			if (attachment.getAuthorId() != 0) {
				tm.setContactDetail(vkUid2ProtocolUid(attachment.getAuthorId(), vkUid, protocolUid));
			}	
			
			/*if (!TextUtils.isEmpty(attachment.getId())) {
				tm.setText(vkm.getText() + "\n" + attachment.getId());
			}*/
		}
		
		return tm;
	}
	
	private static final String vkUid2ProtocolUid(long vkUid, long myVkUid, String protocolUid) {
		return vkUid == myVkUid ? protocolUid : Long.toString(vkUid);
	}

	public static VkMessage textMessage2VkMessage(TextMessage message, boolean isChat) {		
		VkMessage vkm = new VkMessage(0, Long.parseLong(message.getContactUid()), isChat ? 16 : 0, System.currentTimeMillis(), null, message.getText(), null);
		return vkm;
	}

	public static PersonalInfo vkBuddy2PersonalInfo(VkBuddy vkb, byte serviceId, long myVkUid, String ownerUid) {
		if (vkb == null) return null;
		
		PersonalInfo info = new PersonalInfo(serviceId);
		info.setProtocolUid(vkUid2ProtocolUid(vkb.getUid(), myVkUid, ownerUid));

		Bundle bundle = new Bundle();
		bundle.putString(PersonalInfo.INFO_NICK, getNickOfVkBuddy(vkb));
		bundle.putString(PersonalInfo.INFO_FIRST_NAME, vkb.getFirstName());
		bundle.putString(PersonalInfo.INFO_LAST_NAME, vkb.getLastName());
		info.setProperties(bundle);
		
		return info;
	}

	public static List<PersonalInfo> vkChats2PersonalInfoList(List<VkChat> chats, byte serviceId) {
		if (chats == null) return null;
		
		List<PersonalInfo> pinfoList = new ArrayList<PersonalInfo>(chats.size());
		for (VkChat vkChat : chats) {
			PersonalInfo pinfo = vkChat2PersonalInfo(vkChat, serviceId);
			if (pinfo != null) {
				pinfoList.add(pinfo);
			}
		}
		
		return pinfoList;
	}

	private static PersonalInfo vkChat2PersonalInfo(VkChat vkChat, byte serviceId) {
		if (vkChat == null) return null;
		
		PersonalInfo pinfo = new PersonalInfo(serviceId);
		pinfo.setProtocolUid(Long.toString(vkChat.getId()));
		pinfo.setMultichat(true);
		pinfo.getProperties().putString(PersonalInfo.INFO_NICK, vkChat.getTitle());
		
		return pinfo;
	}

	public static MultiChatRoom vkChat2MultiChatRoom(VkChat vkChat, String ownerUid, byte serviceId) {
		if (vkChat == null) return null;
		
		MultiChatRoom chat = new MultiChatRoom(Long.toString(vkChat.getId()), ownerUid, VkConstants.PROTOCOL_NAME, serviceId);
		chat.setName(vkChat.getTitle());
		
		return chat;
	}

	public static List<BuddyGroup> vkChatOccupants2ChatOccupants(VkChat vkChat, List<VkBuddy> occupants, long myId, String ownerUid, Byte serviceId) {
		if (occupants == null) return null;
		
		BuddyGroup moderators = new BuddyGroup(Integer.toString(1), ownerUid, serviceId);
		BuddyGroup all = new BuddyGroup(Integer.toString(0), ownerUid, serviceId);
		moderators.setName("Moderators");
		all.setName("All");
		
		for (VkBuddy vkBuddy : occupants) {
			Buddy buddy = vkBuddy2Buddy(vkBuddy, myId, ownerUid, serviceId);
			if (vkBuddy.getUid() == vkChat.getAdminId()) {
				moderators.getBuddyList().add(buddy);
			} else {
				all.getBuddyList().add(buddy);
			}
		}		
		
		return Arrays.asList(moderators, all);
	}

	public static Message vkChatMessage2Message(long chatId, VkMessage vkm, long myUid, String ownerUid, byte serviceId) {
		if (vkm == null) return null;
		
		//TODO support for other message types
		TextMessage tm = new TextMessage(serviceId, Long.toString(chatId));
		tm.setContactDetail(Long.toString(vkm.getPartnerId()));
		tm.setTime(vkm.getTimestamp());
		tm.setIncoming(true);
		tm.setMessageId(vkm.getMessageId());
		tm.setText(vkm.getText());
		
		for (VkMessageAttachment attachment : vkm.getAttachments()) {
			if (attachment.getAuthorId() != 0) {
				tm.setContactDetail(vkUid2ProtocolUid(attachment.getAuthorId(), myUid, ownerUid));
			}			
		}
		
		return tm;
	}

	public static List<OnlineInfo> vkChats2OnlineInfoList(Set<VkChat> chats, Set<Long> connectedChats, String protocolUid, byte serviceId) {
		if (chats == null) return null;
		
		List<OnlineInfo> infos = new ArrayList<OnlineInfo>(chats.size());
		
		for (VkChat chat : chats) {
			OnlineInfo info = new OnlineInfo(serviceId, Long.toString(chat.getId()));
			
			if (connectedChats != null && connectedChats.contains(chat.getId())) {
				info.getFeatures().putByte(ApiConstants.FEATURE_STATUS, (byte) 0);
			}
			
			infos.add(info);
		}
		
		return infos;
	}
}
