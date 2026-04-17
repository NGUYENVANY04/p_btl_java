// import java.util.List;
// import java.util.Map;

// import org.springframework.stereotype.Repository;
// import org.springframework.web.client.RestTemplate;
// import org.springframework.core.ParameterizedTypeReference;
// import org.springframework.http.*;

// @Repository
// public class DeviceRepository {

// private final RestTemplate restTemplate;

// private final String URL =
// "https://znfxhbrkabxenuzrcogd.supabase.co/rest/v1";
// private final String API_KEY = "sb_secret_NXFJy_AuCYhqJmmJadDKNA_I7N91qFu";

// public DeviceRepository(RestTemplate restTemplate) {
// this.restTemplate = restTemplate;
// }

// private HttpHeaders createHeaders() {
// HttpHeaders headers = new HttpHeaders();
// headers.set("apikey", API_KEY);
// headers.set("Authorization", "Bearer " + API_KEY);
// headers.setContentType(MediaType.APPLICATION_JSON);
// headers.set("Prefer", "return=representation");
// return headers;
// }

// public List<Map<String, Object>> insertDevice(Map<String, Object> device) {

// String url = URL + "/devices";

// HttpEntity<Map<String, Object>> entity = new HttpEntity<>(device,
// createHeaders());

// ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
// url,
// HttpMethod.POST,
// entity,
// new ParameterizedTypeReference<List<Map<String, Object>>>() {
// });

// return response.getBody();
// }

// public List<Map<String, Object>> getDevices() {

// String url = URL + "/devices";

// HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

// ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
// url,
// HttpMethod.GET,
// entity,
// new ParameterizedTypeReference<List<Map<String, Object>>>() {
// });

// return response.getBody();
// }
// }