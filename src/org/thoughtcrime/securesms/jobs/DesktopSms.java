package org.thoughtcrime.securesms.jobs;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.crypto.storage.TextSecureAxolotlStore;
import org.thoughtcrime.securesms.database.TextSecureDirectory;
import org.thoughtcrime.securesms.database.model.SmsMessageRecord;
import org.thoughtcrime.securesms.push.SecurityEventListener;
import org.thoughtcrime.securesms.push.TextSecurePushTrustStore;
import org.thoughtcrime.securesms.sms.IncomingTextMessage;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.Util;
import org.whispersystems.libaxolotl.util.guava.Optional;
import org.whispersystems.textsecure.api.TextSecureMessageSender;
import org.whispersystems.textsecure.api.messages.TextSecureDataMessage;
import org.whispersystems.textsecure.api.push.TextSecureAddress;
import org.whispersystems.textsecure.api.util.InvalidNumberException;

import java.io.IOException;

public class DesktopSms {

	public static final String TAG = PushDecryptJob.class.getSimpleName();

	public static void selfText(String message, Context context)
	{
		TextSecureMessageSender messageSender = new TextSecureMessageSender(BuildConfig.TEXTSECURE_URL,
						new TextSecurePushTrustStore(context),
						TextSecurePreferences.getLocalNumber(context),
						TextSecurePreferences.getPushServerPassword(context),
						new TextSecureAxolotlStore(context),
						BuildConfig.USER_AGENT,
						Optional.<TextSecureMessageSender.EventListener>of(new SecurityEventListener(context)));
		TextSecureAddress address = getPushAddress(TextSecurePreferences.getLocalNumber(context), context);

		TextSecureDataMessage textSecureMessage = TextSecureDataMessage.newBuilder()
				.withBody(message)
				.build();
		try {
			messageSender.sendMessage(address, textSecureMessage);
		}catch(Exception e)
		{

		}
	}

	public static void processDesktopCommand(String message, Context context)
	{
		JSONObject json;
		try {
			json = new JSONObject(message);
			selfText(json.toString(), context);

		} catch(JSONException e)
		{
			Log.w(TAG, e);
		}
	}

	public static void inboundSmsFwd(IncomingTextMessage msg, Context context)
	{
		try {
			JSONObject json = new JSONObject();
			json.put("sender", msg.getSender());
			json.put("recipient", TextSecurePreferences.getLocalNumber(context));
			json.put("timestamp", msg.getSentTimestampMillis());
			json.put("message", msg.getMessageBody());

			selfText(json.toString(), context);
		}catch (JSONException e)
		{
			Log.w(TAG, e);
		}

	}

	public static void outboundSmdFwd(SmsMessageRecord msg, Context context)
	{
		try {
			JSONObject json = new JSONObject();
			json.put("sender", TextSecurePreferences.getLocalNumber(context));
			json.put("recipient", msg.getIndividualRecipient().getNumber());
			json.put("timestamp", msg.getTimestamp());
			json.put("message", msg.getBody().getBody());

			selfText(json.toString(), context);
		}catch (JSONException e)
		{
			Log.w(TAG, e);
		}
	}

	private static TextSecureAddress getPushAddress(String number, Context context){
		try {
			String e164number = Util.canonicalizeNumber(context, number);
			String relay = TextSecureDirectory.getInstance(context).getRelay(e164number);
			return new TextSecureAddress(e164number, Optional.fromNullable(relay));
		} catch(InvalidNumberException e)
		{
			return null;
		}
	}
}
