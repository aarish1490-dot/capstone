package com.dhatchina.dhatchinamart.service;

import com.dhatchina.dhatchinamart.dao.CartDAO;
import com.dhatchina.dhatchinamart.dao.OrderDAO;
import com.dhatchina.dhatchinamart.dao.ProductDAO;
import com.dhatchina.dhatchinamart.dto.SellerOrderView;
import com.dhatchina.dhatchinamart.exception.NotFoundException;
import com.dhatchina.dhatchinamart.exception.ValidationException;
import com.dhatchina.dhatchinamart.model.CartItem;
import com.dhatchina.dhatchinamart.model.Order;
import com.dhatchina.dhatchinamart.model.OrderItem;
import com.dhatchina.dhatchinamart.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    private static final long BUYER_ID = 7L;
    private static final long SELLER_ID = 3L;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private OrderDAO orderDAO;

    @Mock
    private CartDAO cartDAO;

    @Mock
    private ProductDAO productDAO;

    private OrderService orderService;

    @BeforeEach
    void setUp() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        orderService = new OrderService(dataSource, orderDAO, cartDAO, productDAO);
    }

    private CartItem cartItem(long productId, int quantity) {
        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    private Product product(long id, BigDecimal price, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setName("Test Product");
        product.setPrice(price);
        product.setStockQty(stock);
        return product;
    }

    private Order orderWithStatus(String status) {
        Order order = new Order();
        order.setId(55L);
        order.setStatus(status);
        return order;
    }

    @Test
    void placeOrderComputesTotalAndCommits() throws Exception {
        when(cartDAO.findByUserId(BUYER_ID)).thenReturn(List.of(cartItem(1L, 2)));
        when(productDAO.findById(connection, 1L)).thenReturn(
                Optional.of(product(1L, new BigDecimal("1499.00"), 10)));
        when(orderDAO.insert(any(Connection.class), any(Order.class))).thenReturn(55L);
        when(productDAO.decrementStock(connection, 1L, 2)).thenReturn(true);

        Order order = orderService.placeOrder(BUYER_ID);

        assertEquals(55L, order.getId());
        assertEquals(new BigDecimal("2998.00"), order.getTotalAmount(),
                "total must be recomputed from the product price, not trusted from the cart");
        verify(productDAO).decrementStock(connection, 1L, 2);
        verify(cartDAO).clearForUser(connection, BUYER_ID);
        verify(connection).commit();
    }

    @Test
    void placeOrderWithEmptyCartThrowsWithoutOpeningTransaction() throws Exception {
        when(cartDAO.findByUserId(BUYER_ID)).thenReturn(List.of());

        assertThrows(ValidationException.class, () -> orderService.placeOrder(BUYER_ID));

        verify(dataSource, never()).getConnection();
    }

    @Test
    void placeOrderWithMissingProductRollsBack() throws Exception {
        when(cartDAO.findByUserId(BUYER_ID)).thenReturn(List.of(cartItem(1L, 1)));
        when(productDAO.findById(connection, 1L)).thenReturn(Optional.empty());

        ValidationException ex = assertThrows(ValidationException.class, () -> orderService.placeOrder(BUYER_ID));

        assertEquals("A product in your cart is no longer available", ex.getMessage());
        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    @Test
    void placeOrderWithInsufficientStockThrowsFriendlyMessage() throws Exception {
        when(cartDAO.findByUserId(BUYER_ID)).thenReturn(List.of(cartItem(1L, 5)));
        when(productDAO.findById(connection, 1L)).thenReturn(
                Optional.of(product(1L, new BigDecimal("100.00"), 3)));

        ValidationException ex = assertThrows(ValidationException.class, () -> orderService.placeOrder(BUYER_ID));

        assertEquals("Only 3 unit(s) of \"Test Product\" are available in stock", ex.getMessage());
        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    @Test
    void placeOrderWhenStockRanOutDuringTransactionRollsBack() throws Exception {
        when(cartDAO.findByUserId(BUYER_ID)).thenReturn(List.of(cartItem(1L, 1)));
        when(productDAO.findById(connection, 1L)).thenReturn(
                Optional.of(product(1L, new BigDecimal("100.00"), 5)));
        when(productDAO.decrementStock(connection, 1L, 1)).thenReturn(false);

        ValidationException ex = assertThrows(ValidationException.class, () -> orderService.placeOrder(BUYER_ID));

        assertEquals("\"Test Product\" is currently out of stock.", ex.getMessage());
        verify(cartDAO, never()).clearForUser(connection, BUYER_ID);
        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    @Test
    void placeOrderItemInsertFailureRollsBack() throws Exception {
        when(cartDAO.findByUserId(BUYER_ID)).thenReturn(List.of(cartItem(1L, 1), cartItem(2L, 1)));
        when(productDAO.findById(connection, 1L)).thenReturn(
                Optional.of(product(1L, new BigDecimal("10.00"), 5)));
        when(productDAO.findById(connection, 2L)).thenReturn(
                Optional.of(product(2L, new BigDecimal("20.00"), 5)));
        when(orderDAO.insert(any(Connection.class), any(Order.class))).thenReturn(55L);
        doThrow(new RuntimeException("insert failed"))
                .when(orderDAO).insertItem(any(Connection.class), any(OrderItem.class));

        assertThrows(RuntimeException.class, () -> orderService.placeOrder(BUYER_ID));

        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    @Test
    void advanceOrderStatusMovesForwardOneStep() {
        when(orderDAO.belongsToSeller(55L, SELLER_ID)).thenReturn(true);
        when(orderDAO.findById(55L)).thenReturn(Optional.of(orderWithStatus("PENDING")));

        orderService.advanceOrderStatus(SELLER_ID, 55L);

        verify(orderDAO).updateStatus(55L, "CONFIRMED");
    }

    @Test
    void advanceOrderStatusOnTerminalStateIsRejected() {
        when(orderDAO.belongsToSeller(55L, SELLER_ID)).thenReturn(true);
        when(orderDAO.findById(55L)).thenReturn(Optional.of(orderWithStatus("DELIVERED")));

        assertThrows(ValidationException.class, () -> orderService.advanceOrderStatus(SELLER_ID, 55L));

        verify(orderDAO, never()).updateStatus(anyLong(), anyString());
    }

    @Test
    void unrelatedSellerGets404WithoutReadingTheOrder() {
        when(orderDAO.belongsToSeller(55L, SELLER_ID)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> orderService.advanceOrderStatus(SELLER_ID, 55L));

        verify(orderDAO, never()).findById(55L);
    }

    @Test
    void ordersForSellerAttachesOnlyTheirOwnItems() {
        Order order = orderWithStatus("PENDING");
        when(orderDAO.findContainingSeller(SELLER_ID)).thenReturn(List.of(order));
        OrderItem sellerLine = new OrderItem();
        sellerLine.setQuantity(3);
        sellerLine.setUnitPrice(new BigDecimal("10.00"));
        when(orderDAO.findItemsByOrderForSeller(55L, SELLER_ID)).thenReturn(List.of(sellerLine));

        List<SellerOrderView> views = orderService.ordersForSeller(SELLER_ID);

        assertEquals(1, views.size());
        assertEquals(1, views.get(0).getItems().size());
        assertEquals(new BigDecimal("30.00"), views.get(0).getSubtotal());
        verify(orderDAO).findItemsByOrderForSeller(55L, SELLER_ID);
    }
}