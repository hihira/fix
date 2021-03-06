package io.github.hihira;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.MarketDataRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by hhr on 2016/09/10.
 */
public class FixApplication extends MessageCracker implements Application {
    final Logger logger = LoggerFactory.getLogger(FixApplication.class);

    public FixApplication() {
    }

    public void onCreate(SessionID sessionID) {
        logger.info("onCreate : sid={}", sessionID.toString());
    }

    public void onLogon(SessionID sessionID) {
        logger.info("onLogon : sid={}", sessionID.toString());

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
        for (String s : getSymbolStrings()) {
            noRelatedSym.set(new Symbol(s));
            marketDataRequest.addGroup(noRelatedSym);
        }

        try {
            Session.sendToTarget(marketDataRequest, sessionID);
            logger.info("Send MarketDataRequest.");
        } catch (SessionNotFound sessionNotFound) {
            logger.error("Failed to send MarketDataRequest.");
            sessionNotFound.printStackTrace();
        }
    }

    public void onLogout(SessionID sessionID) {
        logger.info("onLogout : sid={}", sessionID.toString());
    }

    public void toAdmin(Message message, SessionID sessionID) {
        logger.info("toAdmin : sid={} : {}", sessionID.toString(), message.toString().replace('\u0001', ' '));

        MsgType msgType = new MsgType();
        StringField field = null;
        try {
            field = message.getHeader().getField(msgType);
        } catch (FieldNotFound fieldNotFound) {
            logger.error("Failed to get message type field.");
            fieldNotFound.printStackTrace();
        }

        // Logon <A>の場合、Passwordを付与
        if (field.valueEquals(MsgType.LOGON)) {
            if (sessionID.getBeginString().compareToIgnoreCase("FIX.4.4") == 0 && !message.isSetField(554)) {
                Password password = new Password(System.getProperty("password", ""));
                message.setField(password);
                message.setField(new ResetSeqNumFlag(true)); // OANDAドキュメントで必須とあるので付与
            }
        }
    }

    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        logger.info("fromAdmin : sid={} : {}", sessionID.toString(), message.toString().replace('\u0001', ' '));

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

    public void toApp(Message message, SessionID sessionID) throws DoNotSend {
        logger.info("toApp : sid={} : {}", sessionID.toString(), message.toString().replace('\u0001', ' '));
    }

    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        logger.info("fromApp : sid={} : {}", sessionID.toString(), message.toString().replace('\u0001', ' '));

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

    public void onMessage(quickfix.fix44.Logon message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        logger.info("Success to logon!");
    }

    public void onMessage(quickfix.fix44.News message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        logger.info("onMessage : message type=" + message.getClass().toString());

        LinesOfText linesOfText = new LinesOfText();
        message.get(linesOfText);

        quickfix.fix44.News.LinesOfText group = new quickfix.fix44.News.LinesOfText();
        Text text = new Text();
        for (int i = 1; i < linesOfText.getValue(); i++) {
            message.getGroup(i, group);
            group.get(text);
            logger.info(text.getValue());
        }
    }

    public void onMessage(quickfix.fix44.Reject message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        logger.info("onMessage : message type=" + message.getClass().toString());

        RefSeqNum refSeqNum = new RefSeqNum();
        RefTagID refTagID = new RefTagID();
        RefMsgType refMsgType = new RefMsgType();
        SessionRejectReason rejectReason = new SessionRejectReason();
        Text text = new Text();
        message.get(refSeqNum);
        try {
            message.get(refTagID);
        } catch (FieldNotFound fieldNotFound) {
            logger.info("RefTagID field is not set.");
        }
        try {
            message.get(refMsgType);
        } catch (FieldNotFound fieldNotFound) {
            logger.info("RefMsgType field is not set.");
        }
        try {
            message.get(rejectReason);
        } catch (FieldNotFound fieldNotFound) {
            logger.info("SessionRejectReason field is not set.");
        }
        try {
            message.get(text);
        } catch (FieldNotFound fieldNotFound) {
            logger.info("Text field is not set.");
        }

        logger.error("Rejected. RefSeqNum={} RefTagID={} RefMsgType={} SessionReject Reason={} Text={}",
                refSeqNum.getValue(), refTagID.getValue(), refMsgType.getValue(), rejectReason.getValue(), text.getValue());
    }

    public void onMessage(quickfix.fix44.Heartbeat message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        logger.info("Receive Heartbeat.");
    }

    public void onMessage(quickfix.fix44.TestRequest message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        logger.info("Receive TestRequest.");
    }

    public void onMessage(quickfix.fix44.MarketDataSnapshotFullRefresh message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        logger.info("onMessage : message type=" + message.getClass().toString());

        Symbol symbol = new Symbol();
        message.get(symbol);
        NoMDEntries noMDEntries = new NoMDEntries();
        message.get(noMDEntries);

        quickfix.fix44.MarketDataSnapshotFullRefresh.NoMDEntries group = new quickfix.fix44.MarketDataSnapshotFullRefresh.NoMDEntries();
        MDEntryType mdEntryType = new MDEntryType();
        MDEntryPx mdEntryPx = new MDEntryPx();
        MDEntrySize mdEntrySize = new MDEntrySize();
        MDEntryDate mdEntryDate = new MDEntryDate();
        MDEntryTime mdEntryTime = new MDEntryTime();
        Text text = new Text();
        MarketDateEntryPair entryPair = new MarketDateEntryPair();
        entryPair.setSymbol(symbol);
        for (int i = 1; i <= noMDEntries.getValue(); i++) {
            message.getGroup(i, group);
            group.get(mdEntryType);
            group.get(mdEntryPx);
            group.get(mdEntrySize);
            group.get(mdEntryDate);
            group.get(mdEntryTime);
            try {
                group.get(text);
                logger.error("Notes on market data entry: {}", text.getValue());
            } catch (FieldNotFound e) {
            }

            entryPair.setPrice(mdEntryType, mdEntryPx);
            entryPair.setSize(mdEntrySize);
            entryPair.setDate(mdEntryDate);
            entryPair.setTime(mdEntryTime);
        }

        System.out.println(entryPair.toString());
    }

    public void onMessage(quickfix.fix44.MarketDataIncrementalRefresh message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        logger.info("onMessage : message type=" + message.getClass().toString());

        NoMDEntries noMDEntries = new NoMDEntries();
        message.get(noMDEntries);

        quickfix.fix44.MarketDataIncrementalRefresh.NoMDEntries group = new quickfix.fix44.MarketDataIncrementalRefresh.NoMDEntries();
        Symbol symbol = new Symbol();
        MDUpdateAction mdUpdateAction = new MDUpdateAction();
        MDEntryType mdEntryType = new MDEntryType();
        MDEntryPx mdEntryPx = new MDEntryPx();
        MDEntrySize mdEntrySize = new MDEntrySize();
        MDEntryDate mdEntryDate = new MDEntryDate();
        MDEntryTime mdEntryTime = new MDEntryTime();
        Text text = new Text();
        MarketDateEntryPair entryPair = new MarketDateEntryPair();
        for (int i = 1; i <= noMDEntries.getValue(); i++) {
            message.getGroup(i, group);
            group.get(symbol);
            group.get(mdUpdateAction);
            group.get(mdEntryType);
            group.get(mdEntryPx);
            group.get(mdEntrySize);
            group.get(mdEntryDate);
            group.get(mdEntryTime);
            try {
                group.get(text);
                logger.error("Notes on market data entry: {}", text.getValue());
            } catch (FieldNotFound e) {
            }

            entryPair.setSymbol(symbol);
            entryPair.setPrice(mdEntryType, mdEntryPx);
            entryPair.setSize(mdEntrySize);
            entryPair.setDate(mdEntryDate);
            entryPair.setTime(mdEntryTime);
        }

        System.out.println(entryPair.toString());
    }

    /**
     * System PropertyかpropertiesファイルからSubscribeするSymbolを取得する<br>
     * System Propertyが優先される<br>
     * どちらにも指定がない場合はUSD/JPYを返却する<br>
     *
     * TODO:Symbolをバリデートする
     *
     * @return symbols
     */
    private String[] getSymbolStrings() {
        String symbols = System.getProperty("symbols", null);
        if (symbols != null) {
            return symbols.split(",");
        }

        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getResourceAsStream("application.properties")) {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return properties.getProperty("symbols", "USD/JPY").split(",");
    }
}
