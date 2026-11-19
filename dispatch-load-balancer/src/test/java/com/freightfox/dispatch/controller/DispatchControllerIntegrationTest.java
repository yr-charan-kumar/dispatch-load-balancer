package com.freightfox.dispatch.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end API tests (controller -> service -> H2 -> optimizer). */
@SpringBootTest
@AutoConfigureMockMvc
class DispatchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void resetData() throws Exception {
        mockMvc.perform(delete("/api/dispatch/data")).andExpect(status().isOk());
    }

    private String postJson(String url, String body, int expectedStatus) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }

    // ---------- happy paths ----------

    @Test
    void specExample_acceptsOrdersAndVehiclesAndReturnsPlan() throws Exception {
        String orders = """
                {"orders":[
                 {"orderId":"ORD001","latitude":12.9716,"longitude":77.5946,"address":"MG Road, Bangalore, Karnataka, India","packageWeight":10,"priority":"HIGH"},
                 {"orderId":"ORD002","latitude":13.0827,"longitude":80.2707,"address":"Anna Salai, Chennai, Tamil Nadu, India","packageWeight":20,"priority":"MEDIUM"}]}""";
        String vehicles = """
                {"vehicles":[
                 {"vehicleId":"VEH001","capacity":100,"currentLatitude":12.9716,"currentLongitude":77.6413,"currentAddress":"Indiranagar, Bangalore, Karnataka, India"},
                 {"vehicleId":"VEH002","capacity":150,"currentLatitude":13.0674,"currentLongitude":80.2376,"currentAddress":"T Nagar, Chennai, Tamil Nadu, India"}]}""";

        mockMvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON).content(orders))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Delivery orders accepted."))
                .andExpect(jsonPath("$.status").value("success"));
        mockMvc.perform(post("/api/dispatch/vehicles").contentType(MediaType.APPLICATION_JSON).content(vehicles))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Vehicle details accepted."))
                .andExpect(jsonPath("$.status").value("success"));

        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispatchPlan.length()").value(2))
                .andExpect(jsonPath("$.dispatchPlan[0].vehicleId").value("VEH001"))
                .andExpect(jsonPath("$.dispatchPlan[0].totalLoad").value(10))
                .andExpect(jsonPath("$.dispatchPlan[0].totalDistance").value("5.06 km"))
                .andExpect(jsonPath("$.dispatchPlan[0].assignedOrders[0].orderId").value("ORD001"))
                .andExpect(jsonPath("$.dispatchPlan[0].assignedOrders[0].address").value("MG Road, Bangalore, Karnataka, India"))
                .andExpect(jsonPath("$.dispatchPlan[1].vehicleId").value("VEH002"))
                .andExpect(jsonPath("$.dispatchPlan[1].totalLoad").value(20))
                .andExpect(jsonPath("$.dispatchPlan[1].assignedOrders[0].orderId").value("ORD002"))
                .andExpect(jsonPath("$.unassignedOrders.length()").value(0))
                .andExpect(jsonPath("$.summary.totalOrders").value(2))
                .andExpect(jsonPath("$.summary.assignedOrders").value(2));
    }

    @Test
    void officialSampleDataset_respectsCapacityAndAccountsForEveryOrder() throws Exception {
        postJson("/api/dispatch/vehicles", Files.readString(Path.of("samples/vehicles.json")), 200);
        postJson("/api/dispatch/orders", Files.readString(Path.of("samples/orders.json")), 200);

        String body = mockMvc.perform(get("/api/dispatch/plan")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode plan = objectMapper.readTree(body);

        int assigned = 0;
        for (JsonNode v : plan.get("dispatchPlan")) {
            assertTrue(v.get("totalLoad").asDouble() <= v.get("capacity").asDouble(),
                    v.get("vehicleId").asText() + " is over capacity");
            assigned += v.get("assignedOrders").size();
        }
        int unassigned = plan.get("unassignedOrders").size();
        assertEquals(30, assigned + unassigned);
        assertEquals(5, plan.get("dispatchPlan").size());
        for (JsonNode u : plan.get("unassignedOrders")) {
            assertEquals("LOW", u.get("order").get("priority").asText());
        }
    }

    @Test
    void reSubmittingAnOrderUpdatesItInsteadOfDuplicating() throws Exception {
        String first = """
                {"orders":[{"orderId":"A","latitude":12.9,"longitude":77.5,"address":"One","packageWeight":10,"priority":"HIGH"}]}""";
        String second = """
                {"orders":[{"orderId":"A","latitude":12.9,"longitude":77.5,"address":"Two","packageWeight":15,"priority":"LOW"}]}""";
        String vehicle = """
                {"vehicles":[{"vehicleId":"V","capacity":100,"currentLatitude":12.9,"currentLongitude":77.5,"currentAddress":"Depot"}]}""";
        postJson("/api/dispatch/orders", first, 200);
        postJson("/api/dispatch/orders", second, 200);
        postJson("/api/dispatch/vehicles", vehicle, 200);

        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(jsonPath("$.summary.totalOrders").value(1))
                .andExpect(jsonPath("$.dispatchPlan[0].assignedOrders[0].address").value("Two"))
                .andExpect(jsonPath("$.dispatchPlan[0].assignedOrders[0].priority").value("LOW"))
                .andExpect(jsonPath("$.dispatchPlan[0].totalLoad").value(15));
    }

    @Test
    void emptySystemReturnsEmptyPlan() throws Exception {
        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispatchPlan.length()").value(0))
                .andExpect(jsonPath("$.unassignedOrders.length()").value(0))
                .andExpect(jsonPath("$.summary.totalOrders").value(0));
    }

    @Test
    void ordersWithoutVehiclesAreAllReportedUnassigned() throws Exception {
        postJson("/api/dispatch/orders", """
                {"orders":[{"orderId":"A","latitude":12.9,"longitude":77.5,"address":"One","packageWeight":10,"priority":"HIGH"}]}""", 200);

        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unassignedOrders.length()").value(1))
                .andExpect(jsonPath("$.unassignedOrders[0].reason").value("NO_VEHICLE_AVAILABLE"));
    }

    @Test
    void overweightOrderIsReportedWithReason() throws Exception {
        postJson("/api/dispatch/vehicles", """
                {"vehicles":[{"vehicleId":"V","capacity":50,"currentLatitude":12.9,"currentLongitude":77.5,"currentAddress":"Depot"}]}""", 200);
        postJson("/api/dispatch/orders", """
                {"orders":[{"orderId":"HEAVY","latitude":12.9,"longitude":77.5,"address":"One","packageWeight":500,"priority":"HIGH"}]}""", 200);

        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(jsonPath("$.unassignedOrders[0].order.orderId").value("HEAVY"))
                .andExpect(jsonPath("$.unassignedOrders[0].reason").value("EXCEEDS_MAX_VEHICLE_CAPACITY"));
    }

    // ---------- validation / error handling ----------

    @Test
    void invalidLatitudeIsRejectedWithFieldLevelMessage() throws Exception {
        String body = """
                {"orders":[{"orderId":"A","latitude":123,"longitude":77.5,"address":"One","packageWeight":10,"priority":"HIGH"}]}""";
        mockMvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("orders[0].latitude"))
                .andExpect(jsonPath("$.errors[0].message").value("latitude must be between -90 and 90"));
    }

    @Test
    void invalidPriorityIsRejected() throws Exception {
        postJson("/api/dispatch/orders", """
                {"orders":[{"orderId":"A","latitude":12.9,"longitude":77.5,"address":"One","packageWeight":10,"priority":"URGENT"}]}""", 400);
    }

    @Test
    void nonPositiveWeightIsRejected() throws Exception {
        postJson("/api/dispatch/orders", """
                {"orders":[{"orderId":"A","latitude":12.9,"longitude":77.5,"address":"One","packageWeight":0,"priority":"HIGH"}]}""", 400);
    }

    @Test
    void missingFieldsAreRejected() throws Exception {
        postJson("/api/dispatch/orders", """
                {"orders":[{"orderId":"A"}]}""", 400);
    }

    @Test
    void emptyOrderListIsRejected() throws Exception {
        postJson("/api/dispatch/orders", "{\"orders\":[]}", 400);
    }

    @Test
    void vehicleWithZeroCapacityIsRejected() throws Exception {
        postJson("/api/dispatch/vehicles", """
                {"vehicles":[{"vehicleId":"V","capacity":0,"currentLatitude":12.9,"currentLongitude":77.5,"currentAddress":"Depot"}]}""", 400);
    }

    @Test
    void malformedJsonIsRejected() throws Exception {
        mockMvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void wrongTypeForNumberIsRejected() throws Exception {
        postJson("/api/dispatch/orders", """
                {"orders":[{"orderId":"A","latitude":"abc","longitude":77.5,"address":"One","packageWeight":10,"priority":"HIGH"}]}""", 400);
    }

    @Test
    void duplicateIdsInOneRequestAreRejected() throws Exception {
        String body = """
                {"orders":[
                 {"orderId":"A","latitude":12.9,"longitude":77.5,"address":"One","packageWeight":10,"priority":"HIGH"},
                 {"orderId":"A","latitude":12.9,"longitude":77.5,"address":"Two","packageWeight":10,"priority":"LOW"}]}""";
        mockMvc.perform(post("/api/dispatch/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Duplicate orderId values in request: [A]"));
    }

    @Test
    void wrongContentTypeIsRejected() throws Exception {
        mockMvc.perform(post("/api/dispatch/orders").contentType(MediaType.TEXT_PLAIN).content("hello"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void wrongHttpMethodIsRejected() throws Exception {
        mockMvc.perform(post("/api/dispatch/plan")).andExpect(status().isMethodNotAllowed());
    }
}
