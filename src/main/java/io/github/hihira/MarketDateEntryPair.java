package io.github.hihira;

import quickfix.field.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by hhr on 2017/05/21.
 */
public class MarketDateEntryPair {

    private BigDecimal bitPrice;
    private BigDecimal offerPrice;
    private String symbol;
    private Double size;
    private Date date;
    private Date time;

    public MarketDateEntryPair() {
    }

    /**
     * @return ex) USD/JPY Sat May 27 05:59:58 JST 2017 111.263 0.008
     */
    @Override
    public String toString() {
        Date date = new Date(this.date.getTime() + time.getTime());
        BigDecimal spread = offerPrice.subtract(bitPrice);
        return symbol + " " + date.toString() + " " + bitPrice.toString() + " " + spread.doubleValue();
    }

    public void setPrice(MDEntryType mdEntryType, MDEntryPx mdEntryPx) {
        switch (mdEntryType.getValue()) {
            case MDEntryType.BID:
                bitPrice = BigDecimal.valueOf(mdEntryPx.getValue());
                break;
            case MDEntryType.OFFER:
                offerPrice = BigDecimal.valueOf(mdEntryPx.getValue());
                break;
            default:
                throw new IllegalArgumentException("MDEntryType=" + mdEntryType.getValue() + " is not allowed");
        }
    }

    public void setSymbol(Symbol symbol) {
        // TODO: 異なっていた場合に例外を投げる
        this.symbol = symbol.getValue();
    }

    public void setSize(MDEntrySize size) {
        // TODO: 異なっていた場合に例外を投げる
        this.size = size.getValue();
    }

    public void setDate(MDEntryDate date) {
        // TODO: 異なっていた場合に例外を投げる
        this.date = date.getValue();
    }

    public void setTime(MDEntryTime time) {
        // TODO: 異なっていた場合に例外を投げる
        this.time = time.getValue();
    }
}
