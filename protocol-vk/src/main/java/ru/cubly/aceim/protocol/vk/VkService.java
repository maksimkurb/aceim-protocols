package ru.cubly.aceim.protocol.vk;

import ru.cubly.aceim.api.IProtocol;
import ru.cubly.aceim.api.dataentity.Buddy;
import ru.cubly.aceim.api.dataentity.BuddyGroup;
import ru.cubly.aceim.api.dataentity.ConnectionState;
import ru.cubly.aceim.api.dataentity.ItemAction;
import ru.cubly.aceim.api.dataentity.Message;
import ru.cubly.aceim.api.dataentity.OnlineInfo;
import ru.cubly.aceim.api.service.AccountService;
import ru.cubly.aceim.api.service.ApiConstants;
import ru.cubly.aceim.api.service.ICoreProtocolCallback;
import ru.cubly.aceim.protocol.vk.internal.VkApiConstants;
import ru.cubly.aceim.protocol.vk.internal.VkServiceInternal;
import android.content.Context;
import android.os.Bundle;

public class VkService extends AccountService {
	
	private final VkServiceInternal internal = new VkServiceInternal(this);

	public VkService(byte serviceId, String protocolUid, ICoreProtocolCallback callback, Context context) {
		super(serviceId, protocolUid, callback, context);
	}
	
	public void loginResult(Bundle result) {
		
		String error = result.getString(VkConstants.EXTRA_ERROR);
		String errorDescription = result.getString(VkConstants.EXTRA_ERROR_DESCRIPTION);
		
		if (error == null && errorDescription == null) {
			String code = result.getString(VkConstants.KEY_CODE);
			
			if (code != null) {
				internal.loginResult(code);
			} else {
				String accessToken = result.getString(VkConstants.KEY_TOKEN);
				long expirationTime = result.getLong(VkConstants.KEY_EXP_TIME_SECONDS);
				long internalUserId = result.getLong(VkConstants.KEY_USER_ID);
				boolean unexpirable = result.getBoolean(VkConstants.KEY_UNEXPIRABLE_TOKEN);
				
				internal.loginResult(accessToken, expirationTime, unexpirable, internalUserId);
			}
		} else {
			
		}
	}

	@Override
	protected ConnectionState getCurrentState() {
		return internal.getConnectionState();
	}

	@Override
	public IProtocol getProtocol() {
		return protocol;
	}

	@Override
	protected void keepaliveRequest() {}

	@Override
	protected void timeoutReconnect() {}

	private final IProtocol protocol = new IProtocol() {
		
		@Override
		public void uploadAccountPhoto(String arg0) {}
		
		@Override
		public void setFeature(String featureId, OnlineInfo arg1) {
			if (featureId.equals(VkApiConstants.FEATURE_GROUPCHATS)) {
				internal.requestAvailableGroupchats();
			} else if (featureId.equals(ApiConstants.FEATURE_XSTATUS)) {
				internal.setStatus(arg1);
			}
		}
		
		@Override
		public void sendTypingNotification(String arg0) {
			internal.typingNotification(arg0);
		}
		
		@Override
		public long sendMessage(Message arg0) { 
			return internal.sendMessage(arg0);
		}
		
		@Override
		public void requestIcon(String arg0) {
			internal.requestIcon(arg0);
		}
		
		@Override
		public void requestFullInfo(String arg0, boolean arg1) {}
		
		@Override
		public void removeAccountPhoto() {}
		
		@Override
		public void messageResponse(Message arg0, boolean arg1) {}
		
		@Override
		public void leaveChatRoom(String chatId) {
			internal.leaveChat(chatId);
		}
		
		@Override
		public void joinChatRoom(String chatId, boolean loadIcons) {
			internal.joinChat(chatId, loadIcons);
		}
		
		@Override
		public void disconnect() {
			internal.logout();
		}
		
		@Override
		public void connect(OnlineInfo arg0) {
			internal.login(arg0);
		}
		
		@Override
		public void cancelFileFransfer(long arg0) {}
		
		@Override
		public void buddyGroupAction(ItemAction arg0, BuddyGroup arg1) {}
		
		@Override
		public void buddyAction(ItemAction arg0, Buddy arg1) {}
	};
}
