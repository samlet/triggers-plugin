package com.bluecc.triggers;

import lombok.Builder;
import lombok.Data;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Configuration
public class OrderFn extends FnBase {
    private static final String MODULE = OrderFn.class.getName();

    @Data
    @Builder
    public static class OrderResult {
        String result;
        String message;
        String orderId;
        String statusId;
    }

    @Bean
    Function<String, OrderResult> salesOrder() {
        return target -> {
            Delegator delegator = getDelegator();
            Map<String, Object> ctx = UtilMisc.<String, Object>toMap("partyId", "DemoCustomer", "orderTypeId", "SALES_ORDER", "currencyUom", "USD",
                    "productStoreId", "9000");

            List<GenericValue> orderPaymentInfo = new LinkedList<>();
            GenericValue orderContactMech = delegator.makeValue("OrderContactMech", UtilMisc.toMap("contactMechId", "9015", "contactMechPurposeTypeId",
                    "BILLING_LOCATION"));
            orderPaymentInfo.add(orderContactMech);

            GenericValue orderPaymentPreference = delegator.makeValue("OrderPaymentPreference", UtilMisc.toMap("paymentMethodId", "9015",
                    "paymentMethodTypeId", "CREDIT_CARD",
                    "statusId", "PAYMENT_NOT_AUTH", "overflowFlag", "N", "maxAmount", new BigDecimal("49.26")));
            orderPaymentInfo.add(orderPaymentPreference);
            ctx.put("orderPaymentInfo", orderPaymentInfo);

            List<GenericValue> orderItemShipGroupInfo = new LinkedList<>();
            orderContactMech.set("contactMechPurposeTypeId", "SHIPPING_LOCATION");
            orderItemShipGroupInfo.add(orderContactMech);

            GenericValue orderItemShipGroup = delegator.makeValue("OrderItemShipGroup", UtilMisc.toMap("carrierPartyId", "UPS", "contactMechId", "9015",
                    "isGift", "N", "shipGroupSeqId", "00001", "shipmentMethodTypeId", "NEXT_DAY"));
            orderItemShipGroupInfo.add(orderItemShipGroup);

            GenericValue orderItemShipGroupAssoc = delegator.makeValue("OrderItemShipGroupAssoc", UtilMisc.toMap("orderItemSeqId", "00001", "quantity",
                    BigDecimal.ONE, "shipGroupSeqId", "00001"));
            orderItemShipGroupInfo.add(orderItemShipGroupAssoc);

            GenericValue orderAdjustment = null;
            orderAdjustment = delegator.makeValue("OrderAdjustment", UtilMisc.toMap("orderAdjustmentTypeId", "SHIPPING_CHARGES", "shipGroupSeqId",
                    "00001", "amount", new BigDecimal("12.45")));
            orderItemShipGroupInfo.add(orderAdjustment);

            orderAdjustment = delegator.makeValue("OrderAdjustment", UtilMisc.toMap("orderAdjustmentTypeId", "SALES_TAX", "orderItemSeqId", "00001",
                    "overrideGlAccountId", "224153",
                    "primaryGeoId", "UT", "shipGroupSeqId", "00001", "sourcePercentage", BigDecimal.valueOf(4.7)));
            orderAdjustment.set("taxAuthGeoId", "UT");
            orderAdjustment.set("taxAuthPartyId", "UT_TAXMAN");
            orderAdjustment.set("taxAuthorityRateSeqId", "9004");
            orderAdjustment.set("amount", BigDecimal.valueOf(1.824));
            orderAdjustment.set("comments", "Utah State Sales Tax");
            orderItemShipGroupInfo.add(orderAdjustment);

            orderAdjustment = delegator.makeValue("OrderAdjustment", UtilMisc.toMap("orderAdjustmentTypeId", "SALES_TAX", "orderItemSeqId", "00001",
                    "overrideGlAccountId", "224153",
                    "primaryGeoId", "UT-UTAH", "shipGroupSeqId", "00001", "sourcePercentage", BigDecimal.valueOf(0.1)));
            orderAdjustment.set("taxAuthGeoId", "UT-UTAH");
            orderAdjustment.set("taxAuthPartyId", "UT_UTAH_TAXMAN");
            orderAdjustment.set("taxAuthorityRateSeqId", "9005");
            orderAdjustment.set("amount", BigDecimal.valueOf(0.039));
            orderAdjustment.set("comments", "Utah County, Utah Sales Tax");
            orderItemShipGroupInfo.add(orderAdjustment);

            orderAdjustment = delegator.makeValue("OrderAdjustment", UtilMisc.toMap("orderAdjustmentTypeId", "SALES_TAX", "orderItemSeqId", "00001",
                    "overrideGlAccountId", "224000",
                    "primaryGeoId", "_NA_", "shipGroupSeqId", "00001", "sourcePercentage", BigDecimal.valueOf(1)));
            orderAdjustment.set("taxAuthGeoId", "_NA_");
            orderAdjustment.set("taxAuthPartyId", "_NA_");
            orderAdjustment.set("taxAuthorityRateSeqId", "9000");
            orderAdjustment.set("amount", BigDecimal.valueOf(0.384));
            orderAdjustment.set("comments", "1% OFB _NA_ Tax");
            orderItemShipGroupInfo.add(orderAdjustment);

            ctx.put("orderItemShipGroupInfo", orderItemShipGroupInfo);

            List<GenericValue> orderAdjustments = new LinkedList<>();
            orderAdjustment = delegator.makeValue("OrderAdjustment", UtilMisc.toMap("orderAdjustmentTypeId", "PROMOTION_ADJUSTMENT",
                    "productPromoActionSeqId", "01", "productPromoId", "9011", "productPromoRuleId", "01", "amount", BigDecimal.valueOf(-3.84)));
            orderAdjustments.add(orderAdjustment);
            ctx.put("orderAdjustments", orderAdjustments);

            List<GenericValue> orderItems = new LinkedList<>();
            GenericValue orderItem = delegator.makeValue("OrderItem", UtilMisc.toMap("orderItemSeqId", "00001", "orderItemTypeId", "PRODUCT_ORDER_ITEM",
                    "prodCatalogId", "DemoCatalog", "productId", "GZ-2644", "quantity", BigDecimal.ONE, "selectedAmount", BigDecimal.ZERO));
            orderItem.set("isPromo", "N");
            orderItem.set("isModifiedPrice", "N");
            orderItem.set("unitPrice", new BigDecimal("38.4"));
            orderItem.set("unitListPrice", new BigDecimal("48.0"));
            orderItem.set("statusId", "ITEM_CREATED");
            orderItems.add(orderItem);

            orderItem = delegator.makeValue("OrderItem", UtilMisc.toMap("orderItemSeqId", "00002", "orderItemTypeId", "PRODUCT_ORDER_ITEM",
                    "prodCatalogId", "DemoCatalog", "productId", "GZ-1006-1", "quantity", BigDecimal.ONE, "selectedAmount", BigDecimal.ZERO));
            orderItem.set("isPromo", "N");
            orderItem.set("isModifiedPrice", "N");
            orderItem.set("unitPrice", new BigDecimal("1.99"));
            orderItem.set("unitListPrice", new BigDecimal("5.99"));
            orderItem.set("statusId", "ITEM_CREATED");
            orderItems.add(orderItem);

            ctx.put("orderItems", orderItems);

            List<GenericValue> orderTerms = new LinkedList<>();
            ctx.put("orderTerms", orderTerms);

            GenericValue orderContactMec = delegator.makeValue("OrderContactMech");
            orderContactMec.set("contactMechPurposeTypeId", "SHIPPING_LOCATION");
            orderContactMec.set("contactMechId", "10000");

            ctx.put("placingCustomerPartyId", "DemoCustomer");
            ctx.put("endUserCustomerPartyId", "DemoCustomer");
            ctx.put("shipToCustomerPartyId", "DemoCustomer");
            ctx.put("billToCustomerPartyId", "DemoCustomer");
            ctx.put("billFromVendorPartyId", "Company");

            ctx.put("userLogin", getUserLogin("system"));
            return storeAndGetOrderResult(ctx);
        };
    }

    @Bean
    Function<String, OrderResult> purchaseOrder() {
        return target -> {
            Delegator delegator = getDelegator();
            Map<String, Object> ctx = new HashMap<>();
            ctx.put("partyId", "Company");
            ctx.put("orderTypeId", "PURCHASE_ORDER");
            ctx.put("currencyUom", "USD");
            ctx.put("productStoreId", "9000");

            GenericValue orderItem = delegator.makeValue("OrderItem", UtilMisc.toMap("orderItemSeqId", "00001", "orderItemTypeId",
                    "PRODUCT_ORDER_ITEM", "prodCatalogId", "DemoCatalog", "productId", "GZ-1000", "quantity", new BigDecimal("2"), "isPromo", "N"));
            orderItem.set("unitPrice", new BigDecimal("1399.5"));
            orderItem.set("unitListPrice", BigDecimal.ZERO);
            orderItem.set("isModifiedPrice", "N");
            orderItem.set("statusId", "ITEM_CREATED");
            List<GenericValue> orderItems = new LinkedList<>();
            orderItems.add(orderItem);
            ctx.put("orderItems", orderItems);

            GenericValue orderContactMech = delegator.makeValue("OrderContactMech", UtilMisc.toMap("contactMechPurposeTypeId",
                    "SHIPPING_LOCATION", "contactMechId", "9000"));
            List<GenericValue> orderContactMechs = new LinkedList<>();
            orderContactMechs.add(orderContactMech);
            ctx.put("orderContactMechs", orderContactMechs);

            GenericValue orderItemContactMech = delegator.makeValue("OrderItemContactMech", UtilMisc.toMap("contactMechPurposeTypeId",
                    "SHIPPING_LOCATION", "contactMechId", "9000", "orderItemSeqId", "00001"));
            List<GenericValue> orderItemContactMechs = new LinkedList<>();
            orderItemContactMechs.add(orderItemContactMech);
            ctx.put("orderItemContactMechs", orderItemContactMechs);

            GenericValue orderItemShipGroup = delegator.makeValue("OrderItemShipGroup", UtilMisc.toMap("carrierPartyId", "UPS",
                    "contactMechId", "9000", "isGift", "N", "maySplit", "N", "shipGroupSeqId", "00001", "shipmentMethodTypeId", "NEXT_DAY"));
            orderItemShipGroup.set("carrierRoleTypeId", "CARRIER");
            List<GenericValue> orderItemShipGroupInfo = new LinkedList<>();
            orderItemShipGroupInfo.add(orderItemShipGroup);
            ctx.put("orderItemShipGroupInfo", orderItemShipGroupInfo);

            List<GenericValue> orderTerms = new LinkedList<>();
            ctx.put("orderTerms", orderTerms);

            List<GenericValue> orderAdjustments = new LinkedList<>();
            ctx.put("orderAdjustments", orderAdjustments);

            ctx.put("billToCustomerPartyId", "Company");
            ctx.put("billFromVendorPartyId", "DemoSupplier");
            ctx.put("shipFromVendorPartyId", "Company");
            ctx.put("supplierAgentPartyId", "DemoSupplier");
            ctx.put("userLogin", getUserLogin("system"));

            return storeAndGetOrderResult(ctx);
        };
    }

    private OrderResult storeAndGetOrderResult(Map<String, Object> ctx) {
        try {
            Map<String, Object> resp = getDispatcher().runSync("storeOrder", ctx);
            if (ServiceUtil.isError(resp)) {
                String msg = ServiceUtil.getErrorMessage(resp);
                Debug.logError(msg, MODULE);
                return OrderResult.builder()
                        .result("fail")
                        .message(msg)
                        .build();
            }

            String orderId = (String) resp.get("orderId");
            String statusId = (String) resp.get("statusId");
            // assertNotNull(orderId);
            // assertNotNull(statusId);
            return OrderResult.builder()
                    .result("ok")
                    .orderId(orderId)
                    .statusId(statusId)
                    .build();
        } catch (GenericServiceException e) {
            e.printStackTrace();
            return OrderResult.builder()
                    .result("fail")
                    .message(e.getMessage())
                    .build();
        }
    }
}
