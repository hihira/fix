package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.News;

/**
 * Created by hhr on 2016/09/10.
 */
public class FixApplication extends MessageCracker implements Application {
    final Logger logger = LoggerFactory.getLogger(FixApplication.class);
    private final SessionSettings settings;

    public FixApplication(SessionSettings settings) {
        this.settings = settings;
    }

    public void onCreate(SessionID sessionID) {
        logger.warn("■■■onCreate : SessionID=" + sessionID.toString());
    }

    public void onLogon(SessionID sessionID) {
        logger.warn("■■■onLogon : SessionID=" + sessionID.toString());

        MarketDataRequest marketDataRequest = new MarketDataRequest(
                new MDReqID(Long.toString(System.currentTimeMillis())), // とりま、ユニーク値としてUnixタイムスタンプのミリ秒を使用
                new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES),
                new MarketDepth(1)); // OANDAは1のみサポート)
        marketDataRequest.setField(new MDUpdateType(MDUpdateType.INCREMENTAL_REFRESH));

        MarketDataRequest.NoMDEntryTypes noMDEntryTypes = new MarketDataRequest.NoMDEntryTypes();
        noMDEntryTypes.set(new MDEntryType(MDEntryType.BID));
        marketDataRequest.addGroup(noMDEntryTypes);
        noMDEntryTypes.set(new MDEntryType(MDEntryType.OFFER));
        marketDataRequest.addGroup(noMDEntryTypes);

        MarketDataRequest.NoRelatedSym noRelatedSym = new MarketDataRequest.NoRelatedSym();
        Symbol symbol = new Symbol("USD/JPY");
        noRelatedSym.set(symbol);
        marketDataRequest.addGroup(noRelatedSym);

        try {
            Session.sendToTarget(marketDataRequest, sessionID);
        } catch (SessionNotFound sessionNotFound) {
            sessionNotFound.printStackTrace();
        }

        logger.info("Send MarketDataRequest : " + marketDataRequest.toString().replace('\u0001', ' '));
    }

    public void onLogout(SessionID sessionID) {
        logger.warn("■■■onLogout : SessionID=" + sessionID.toString());
    }

    public void toAdmin(Message message, SessionID sessionID) {
        logger.warn("■■■toAdmin : SessionID=" + sessionID.toString());
        logger.warn("^^^toAdmin : message=" + message.toString());

        MsgType msgType = new MsgType();
        StringField field = null;
        try {
            field = message.getHeader().getField(msgType);
        } catch (FieldNotFound fieldNotFound) {
            fieldNotFound.printStackTrace();
        }

        // Logon <A>の場合、Passwordを付与
        if (field.valueEquals(MsgType.LOGON)) {
            if (sessionID.getBeginString().compareToIgnoreCase("FIX.4.4") == 0 && !message.isSetField(554)) {
                Password password = null;
                try {
                    password = new Password(settings.getString(sessionID, "Password"));
                } catch (ConfigError configError) {
                    configError.printStackTrace();
                } catch (FieldConvertError fieldConvertError) {
                    fieldConvertError.printStackTrace();
                }

                message.setField(password);
                message.setField(new ResetSeqNumFlag(true)); // OANDAドキュメントで必須とあるので付与
            }
        }
    }

    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        logger.warn("■■■fromAdmin : SessionID=" + sessionID.toString());
        logger.warn("^^^fromAdmin : message=" + message.toString());
    }

    public void toApp(Message message, SessionID sessionID) throws DoNotSend {
        logger.warn("■■■toApp : SessionID=" + sessionID.toString());
        logger.warn("^^^toApp : message=" + message.toString());
    }

    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        logger.warn("■■■fromApp : SessionID=" + sessionID.toString());
        logger.warn("^^^fromApp : message=" + message.toString());

        try {
            crack(message, sessionID);
        } catch (UnsupportedMessageType unsupportedMessageType) {
            unsupportedMessageType.printStackTrace();
        } catch (FieldNotFound fieldNotFound) {
            fieldNotFound.printStackTrace();
        } catch (IncorrectTagValue incorrectTagValue) {
            incorrectTagValue.printStackTrace();
        }
    }

    public void onMessage(quickfix.fix44.News message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        logger.warn("■■■onMessage : SessionID=" + sessionID.toString());
        logger.warn("^^^onMessage : message=" + message.toString());

        LinesOfText linesOfText = new LinesOfText();
        message.get(linesOfText);

        News.LinesOfText group = new News.LinesOfText();
        Text text = new Text();
//        for (int i = 1; i < linesOfText.getValue(); i++) {
//            message.getGroup(i, group);
//            group.get(text);
//            logger.info(text.getValue());
//        }

        // TODO: なんか開発環境が変。getGroupできない
        String messageString = message.getString(58);
        logger.info(messageString);
    }
}
