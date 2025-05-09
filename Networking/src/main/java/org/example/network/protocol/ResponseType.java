package org.example.network.protocol;

public enum ResponseType {
    OK,
    ERROR,
    MATCHES,
    MATCH,
    USER,
    TICKETS,
    PRICE_RANGE,
    AVAILABLE_TICKETS,
    TICKET_SALE_RESULT,
    IS_TICKET_SELLER,

    MATCH_UPDATED, USER_TICKETS_CHANGED, TICKET_SOLD,
    CONSOLIDATED_UPDATE

}