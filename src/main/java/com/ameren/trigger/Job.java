package com.ameren.trigger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.omg.IOP.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ameren.outage.core.model.Order;
import com.ameren.outage.core.model.Outage;
import com.ameren.outage.core.model.PilotOperatingCenter;
import com.ameren.outage.core.model.PilotOperatingCenterWrapper;
import com.ameren.outage.core.model.RestResponse;
import com.ameren.outage.core.model.Snapshot;
import com.ameren.outage.core.model.State;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import model.DbDeviceOutageReader;
import model.DbOrderDetailReader;
import model.DbOrders;
import model.DbTransformers;
import model.OrderConverter;
import model.OrderPayload;
import model.SnapshotManager;
import model.SnapshotPayload;
import model.StatePayload;

@Service
public class Job {
	Logger logger = LoggerFactory.getLogger(Job.class);
	private static final String ORDER_FILE = 
//			"src/main/resources/orderT.json";
			"src/main/resources/order-detail-prod-0810-0814.json";
	private static final String DEVICE_FILE =
//			"src/main/resources/deviceT.json";
			"src/main/resources/device-outage-prod-0810-0814-4.json";
	private SnapshotManager snapshotManager = new SnapshotManager();
	
	@Autowired
	private RestTemplate restTemplate;
	private String getSnapshotUrlString_DEV = "https://hawscorpt.ameren.com:8443/sys/eadms/outage/v1/snapshot";
	private String postOutageToOchUrlString_DEV = "https://outage-dev.ameren.com/snapshot/process";
	private String postOutageToOchUrlString_QA = "https://outage-qa.ameren.com/snapshot/process";
	private Map<String, String> staticOpCodeName = new HashMap<>();
	private static HttpHeaders QA_Header = new HttpHeaders();
	private static HttpHeaders PROD_Header = new HttpHeaders();
	private Outage fixedOutagePayload = null;
	
	static {
		QA_Header.setContentType(MediaType.APPLICATION_JSON);
	    QA_Header.add("client_id", "7eddf5ecc4a9459dbf569a04236bed43");
	    QA_Header.add("client_secret", "facc07B5a71C4df380b69f6cb3614C05");
	    
	    PROD_Header.setContentType(MediaType.APPLICATION_JSON);
	    PROD_Header.add("client_id", "009f6dd57238424a8f8a251ec0489835");
	    PROD_Header.add("client_secret", "b132fF2D553043EeAC1d94f330D2c556");
	}
    
	public Map<String, String> getOpCodeName() {
		if(staticOpCodeName.isEmpty()) {
			staticOpCodeName = new HashMap<>();
			PilotOperatingCenterWrapper opCenterWrapper;
			try {
				opCenterWrapper = readJsonFileIntoPojo("src/main/resources/opCenters-prod.json",PilotOperatingCenterWrapper.class);
				for(PilotOperatingCenter op: opCenterWrapper.getPilotOperatingCenters()) {
					staticOpCodeName.put(op.getOpCenterCode(), op.getOpCenterName());
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
		return staticOpCodeName;
	}
	
	public Outage getFixedPayload() {
		if(fixedOutagePayload == null) {
			try {
				fixedOutagePayload = readJsonFileIntoPojo("src/main/resources/outage-payload.json",Outage.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return fixedOutagePayload;
	}
	
	public void doTheJob() {
		doTheJob(getSnapshotUrlString_DEV);
	}
	
	private String getPayload(String url) {
//		HttpHeaders headers = new HttpHeaders();
//	    headers.setContentType(MediaType.APPLICATION_JSON);
//	    headers.add("client_id", "7eddf5ecc4a9459dbf569a04236bed43");
//	    headers.add("client_secret", "facc07B5a71C4df380b69f6cb3614C05");
	    
		HttpEntity requestEntity = new HttpEntity(QA_Header);
		ResponseEntity<String> snapshot = restTemplate.exchange(getSnapshotUrlString_DEV, HttpMethod.GET, requestEntity , String.class);
		String body = snapshot.getBody();
		
		int start = body.indexOf('{', 5);
		int end = body.lastIndexOf('}');
		String postPayload = body.substring(start, end);
		return postPayload;
	}
	
	public void doTheJob(String postTo) {
//		HttpHeaders headers = new HttpHeaders();
//	    headers.setContentType(MediaType.APPLICATION_JSON);
//	    headers.add("client_id", "7eddf5ecc4a9459dbf569a04236bed43");
//	    headers.add("client_secret", "facc07B5a71C4df380b69f6cb3614C05");
//	    
//		HttpEntity requestEntity = new HttpEntity(headers);
//		ResponseEntity<String> snapshot = restTemplate.exchange(getSnapshotUrlString_DEV, HttpMethod.GET, requestEntity , String.class);
//		String body = snapshot.getBody();
//		
//		int start = body.indexOf('{', 5);
//		int end = body.lastIndexOf('}');
//		String postPayload = body.substring(start, end);
//		System.out.println(postPayload);
		
		String postPayload = getPayload(postTo);
		HttpEntity<String> postEntity = new HttpEntity<String>(postPayload, QA_Header);
		ResponseEntity<String> responseEntity = restTemplate.exchange(postTo, HttpMethod.POST, postEntity, String.class);
//		System.out.println(responseEntity.getBody());
	}
	
	public Outage getSnapshot(String env) {
		HttpHeaders headers = null;
	    String url = "";
	    if("qa".equalsIgnoreCase(env)) {
	    	url = "https://hawscorpt.ameren.com:8443/sys/eadms/outage/v1/snapshot";
	    	headers = QA_Header;
	    }else if ("prod".equalsIgnoreCase(env)) {
	    	url = "https://hawscorpp.ameren.com:8443/sys/eadms/outage/v1/snapshot";
	    	headers = PROD_Header;
	    }
	    
		HttpEntity requestEntity = new HttpEntity(headers);
		ResponseEntity<String> snapshot = restTemplate.exchange(url, HttpMethod.GET, requestEntity , String.class);
		
		OutageWrapperForTest outageWrapper;
		try {
			outageWrapper = mapper.readValue(snapshot.getBody(), OutageWrapperForTest.class);
			return outageWrapper.getOutage();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    return null;
	}

	private StringBuilder result;
	public StringBuilder analyze(Outage outage) {
		result = new StringBuilder();
		Map<String, Order> snapshotOrderMap = new HashMap<>();
		int totalNumberOfOrdersFromSnapshot = 0;
		for(State state: outage.getStates()) {
			totalNumberOfOrdersFromSnapshot += state.getOrders().size();
			logStateOrders(state);
//			snapshotOrderMap.putAll(processStateOrders(state));
		}

//		System.out.println("Total number of orders from snapshot: " + totalNumberOfOrdersFromSnapshot);
		
//		checkSnapshotDbDifference(snapshotOrderMap);
		return result;
	}
	
	private void logStateOrders(State state) {
//		throws JsonParseException, JsonMappingException, IOException {
	
		result.append("STATE: " + state.getState()).append("\r\n");
		List<Order> orders = state.getOrders();
		Map<DeviceOutageStatus, List<Order>> snapshotStateOrderMap = new HashMap<DeviceOutageStatus, List<Order>>();
		//count of valid orders which has call and not PA for specific state
//			int validOrderCount = 0;
		//count of customers from all orders in snapshot
		int snapshotCustomerCount = 0;			
		//Snapshot transformer Level outages transformers for retrive CSS customers(count)
		Set<String> snapshotTransformerLevelOutageTransformers = new HashSet<>();
		// count of valid single premise orders for specific state;
		int snapshotSinglePremisOrderCount = 0;
		for(Order order: orders) {
			
//			snapshotOrderNumbers.add(order.getOrderNumber());
			DeviceOutageStatus deviceOutageStatus = identifyDeviceStatusFromOrder(order);
//			snapshotStateOrderMap.put(order.getCauseCode(), order);
			snapshotCustomerCount += order.getOutageSize();
//				validOrderCount ++;
			//Check and count single Premise Outage
//			if(order.getPremiseNumber() != null) {
//				if(order.getOutageSize() == 1) {
//					snapshotSinglePremisOrderCount++;
//				}else {
//					System.err.println("Order: " + order.getOrderNumber() + " has premise: " + order.getPremiseNumber() + ", but size is not 1");
//				}
//			}else { // add to transformers for retrieving real customers
//				snapshotTransformerLevelOutageTransformers.addAll(order.getTransformers());
//			}
			List<Order> orderList = snapshotStateOrderMap.get(deviceOutageStatus);
			if(orderList == null) {
				orderList = new ArrayList<>();
				snapshotStateOrderMap.put(deviceOutageStatus, orderList);
			}
			orderList.add(order);
			
		}
		result.append("\tnumber of orders from snapshot: " + state.getOrders().size() + ", number of customers: " + snapshotCustomerCount).append("\r\n");
//		System.out.println("\tnumber of customers from snapshot: " + snapshotCustomerCount);
		for(DeviceOutageStatus status: snapshotStateOrderMap.keySet()) {
			if(status != DeviceOutageStatus.PPO_INITIATED) {
				result.append("\tnumber of orders as " + status + ":" + snapshotStateOrderMap.get(status).size() + ", number of customers:" + getOutageSize(snapshotStateOrderMap.get(status))).append("\r\n");
			}
		}
		if(snapshotStateOrderMap.get(DeviceOutageStatus.PPO_INITIATED) != null) {
			List<Order> ppoInitiated = snapshotStateOrderMap.get(DeviceOutageStatus.PPO_INITIATED);
			result.append("\tnumber of orders as " + DeviceOutageStatus.PPO_INITIATED + ":" + ppoInitiated.size() + ", number of customers:" + getOutageSize(ppoInitiated)).append("\r\n");
			logEligibleOrders(ppoInitiated);
		}
		
//		System.out.println("\tnumber of single prmise order count: " + snapshotSinglePremisOrderCount);
		/*
		StringBuilder requestStringBuilder = new StringBuilder("{\"transformers\":[");
		for(String t: snapshotTransformerLevelOutageTransformers) {
			requestStringBuilder.append("{\"transformerNumber\": \"").append(t).append("\"},");
		}
		
		String getCssCustomersByTransformersRequest = new String(requestStringBuilder);
		getCssCustomersByTransformersRequest = getCssCustomersByTransformersRequest.substring(0, getCssCustomersByTransformersRequest.length() -1);
		getCssCustomersByTransformersRequest = getCssCustomersByTransformersRequest + "]}";
		System.out.println("\t\t" + getCssCustomersByTransformersRequest);
		
		logSnapshotOrders(snapshotStateOrderMap);
		*/
//		logOrdersByCauseCode(snapshotStateOrderMap);
//		return snapshotStateOrderMap;
	}
	
	private Map<String, List<Order>> opOrders;
	private ObjectMapper mapper = new ObjectMapper();
	private boolean autoRun = false;
	
	
	private void logEligibleOrders(List<Order> list) {
	
		opOrders = new HashMap<>();
		for(Order order : list) {
			List<Order> orders = opOrders.get(order.getOperatingCenter());
			if(orders == null) {
				orders = new ArrayList();
				opOrders.put(order.getOperatingCenter(), orders);
			}
			orders.add(order);
		}
		
		Set<Entry<String, List<Order>>> entrySet = opOrders.entrySet();
		for(Entry<String, List<Order>> entry: opOrders.entrySet()) {
			int opSize = 0;
			for(Order order: entry.getValue()) {
				opSize += order.getOutageSize();
			}
			result.append("\t\t" + getOpCodeName().get(entry.getKey()) + "(" + opSize+ "): ");
			for(Order order: entry.getValue()) {
				result.append(order.getOrderNumber() + "(" + order.getOutageSize() + "), ");
			}
			result.append("\r\n");
		}
		
	}

	protected <V> V readJsonFileIntoPojo(String pathname, Class<V> clazz)
			throws IOException, JsonParseException, JsonMappingException {
		String json = new String(Files.readAllBytes(new File(pathname).toPath()));
		RestResponse restWrapperForSnapshot = mapper.readValue(json, RestResponse.class);
		V v = mapper.convertValue(restWrapperForSnapshot.getData(), clazz);
		return v;
	}
	
	private DeviceOutageStatus identifyDeviceStatusFromOrder(final Order outageOrder) {
//			throws JsonParseException, JsonMappingException, IOException {
        if ("PA".equalsIgnoreCase(outageOrder.getCauseCode())) {
            return DeviceOutageStatus.PPO_ALREADY_SENT;
        }
//        PilotOperatingCenterWrapper opCenterWrapper = readJsonFileIntoPojo("src/test/resources/opCenters-prod.json",PilotOperatingCenterWrapper.class);
        if (outageOrder.hasCall()) {
            if (getOpCodeName().keySet().contains(outageOrder.getOperatingCenter())) {
//        	if (opCenterWrapper.getPilotOperatingCenters().stream().anyMatch(t -> t.getOpCenterCode().equals(outageOrder.getOperatingCenter()))) {
                return DeviceOutageStatus.PPO_INITIATED;
            } else {
                return DeviceOutageStatus.NOT_IN_ACTIVE_OC;
            }
        } else {
            return DeviceOutageStatus.PPO_NOT_VERIFIED;
        }
    }
	
	private int getOutageSize(List<Order> list) {
		int count = 0;
		for(Order order: list) {
			count += order.getOutageSize();
		}
		return count;
	}

	public void sendToQA(String payload) {
		HttpEntity<String> postEntity = new HttpEntity<String>(payload, QA_Header);
		ResponseEntity<String> responseEntity = restTemplate.exchange(postOutageToOchUrlString_QA, HttpMethod.POST, postEntity, String.class);
	}
	
public void sendToDev(String payload) {
	HttpHeaders httpHeaders = new HttpHeaders();
	httpHeaders.add("Content-Type", "application/json");
	HttpEntity<String> postEntity = new HttpEntity<String>(payload,httpHeaders);
	ResponseEntity<String> responseEntity = restTemplate.exchange(postOutageToOchUrlString_DEV, HttpMethod.POST, postEntity, String.class);
	}


	private static Map<Long, String> orderIdNumberMap = new HashMap<>();
	private static Map<String, List<OrderPayload>> orderNumber_OrderList = new HashMap<>();
	private static Map<Long, List<DbDeviceOutageReader>> transformerMap = new HashMap<>();
	private static int runCount= 1;
	
	public void runLoadTest() {
		String payload = getPayload(runCount);
		if(payload != null && payload.length() > 10) {
			sendToDev(payload);
			runCount++;
		}
	}
	
	public String getPayload(int whichRun) {
		Long currentSnapshotId = snapshotManager.getCurrentSnapshotId(whichRun);
		populateSnapshot(currentSnapshotId, snapshotManager, transformerMap);		
		try {
			System.out.println("Get current snapshot: "+ whichRun + " - " + currentSnapshotId);
			String payload = mapper.writeValueAsString(snapshotManager.getSnapshot(currentSnapshotId));
			logger.info("Get current snapshot: {} - {}",whichRun ,currentSnapshotId);
			logger.info(payload);
			return payload;
		} catch (JsonProcessingException e) {
			System.out.println("Failed to getPayload test");
			e.printStackTrace();
		}
		return null;
	}

	public boolean autoRun() {
		if(autoRun) {
			runLoadTest();
		}
		return autoRun;
	}
	
	public void prepareLoadTestData() {
		snapshotManager = new SnapshotManager();
		StringBuffer sb = new StringBuffer();
//		Set<Long> snapshotIds = new HashSet();
		try {
//			//order_detail_id, List of devices
//			Map<Long, List<DbDeviceOutageReader>> transformerMap = prepareTransformers();
//			System.out.println("There are "+ transformerMap.size() + " order details has trasnfermers");
			
			String json = new String(
					Files.readAllBytes(new File(ORDER_FILE).toPath()));
//					Files.readAllBytes(new File("src/main/resources/Test.json").toPath()));
			DbOrders dbOrderList = mapper.readValue(json, DbOrders.class);
			System.out.println("There are " + dbOrderList.getOrders().size() + " order details in order_detal table");

//			System.out.println(mapper.writeValueAsString(OrderConverter.convert( dbOrderList.getOrders().get(0))));
			
			
//			int count = prepareOrderDetails(snapshotManager, sb, snapshotIds, transformerMap, dbOrderList);

			int count = 0;
//			Set<Long> noDeviceOrders = new HashSet<>();
			
			for(DbOrderDetailReader dbOrder : dbOrderList.getOrders()) {
//				snapshotIds.add(dbOrder.getSnapshot_id());
				OrderPayload orderPayload = OrderConverter.convert(dbOrder);
//				populateDevices(transformerMap, orderPayload, dbOrder.getId());
				
				count++;
				
				SnapshotPayload snapshot = snapshotManager.getSnapshot(dbOrder.getSnapshot_id());
				if(dbOrder.getOrder_number() > 5000000) {
					orderPayload.setState("IL");
					snapshot.getState("IL").add(orderPayload);
				}else {
					snapshot.getState("MO").add(orderPayload);
					orderPayload.setState("MO");
				}
				
//				orderIdNumberMap.put(orderPayload.getId(), orderPayload.getOrderNumber());
				//create order Id map to order Number;
				buildOrderNumberMapOrderDetails(orderPayload);
			}
			snapshotManager.loadTestSetupFinished();
			
			List<Long> snapshotIds = snapshotManager.getSnapshotIds();
			System.out.println("There are " + snapshotManager.getSnapshotIds().size() + " snapshots") ;
			System.out.println("There are " + count + " order details without devices: ");
			System.out.println(""+ new String(sb));
			System.out.println("There are " + snapshotIds.size() + " snapshots");
			
			//order_detail_id, List of devices
			transformerMap = prepareTransformers();
//			System.out.println("There are "+ transformerMap.size() + " order details has trasnfermers");
			
//			List<Long> snapshotList = new ArrayList<>(snapshotIds);
//			Collections.sort(snapshotList);
			/*
			for(int i = 1025; i < 1035; i++) {
//			for(int i = 500; i <505; i++) {				
				long currentSnapshotId = snapshotIds.get(i);
				populateSnapshot(currentSnapshotId, snapshotManager, transformerMap);		
				String payload = mapper.writeValueAsString(snapshotManager.getSnapshot(currentSnapshotId));
				System.out.println(payload);
				Outage outage = mapper.readValue(payload, Outage.class);
				
			}
			for(int i = 500; i <505; i++) {
			runLoadTest();
			}
			*/
			System.out.println("Done");
		} catch (Exception e) {
			System.out.println("failed to load");
			e.printStackTrace();
		}
	}

	private void buildOrderNumberMapOrderDetails(OrderPayload orderPayload) {
		orderIdNumberMap.put(orderPayload.getId(), orderPayload.getOrderNumber());
		List<OrderPayload> list = orderNumber_OrderList.get(orderPayload.getOrderNumber());
		if(list ==null) {
			list = new ArrayList<>();
			orderNumber_OrderList.put(orderPayload.getOrderNumber(), list);
		}
		list.add(orderPayload);
	}
	
	
	private int prepareOrderDetails(SnapshotManager snapshotManager, StringBuffer sb, Set<Long> snapshotIds,
			Map<Long, List<DbDeviceOutageReader>> transformerMap, DbOrders dbOrderList) {
		long forPrintSnapshotId = 0;
		int count = 0;
//			Set<Long> noDeviceOrders = new HashSet<>();
		
		for(DbOrderDetailReader dbOrder:dbOrderList.getOrders()) {
			snapshotIds.add(dbOrder.getSnapshot_id());
			OrderPayload orderPayload = OrderConverter.convert(dbOrder);
			populateDevices(transformerMap, orderPayload, dbOrder.getId());
			
			if(orderPayload.getTransformers() == null || orderPayload.getTransformers().isEmpty()) {
//					System.out.println("There is no transformers for the order: " + dbOrder.getId());
				sb.append(dbOrder.getId()).append(",");
				count++;
				continue;
			}
			forPrintSnapshotId = dbOrder.getSnapshot_id();
			SnapshotPayload snapshot = snapshotManager.getSnapshot(dbOrder.getSnapshot_id());
			if(dbOrder.getOrder_number() > 5000000) {
				orderPayload.setState("IL");
				snapshot.getState("IL").add(orderPayload);
			}else {
				snapshot.getState("MO").add(orderPayload);
				orderPayload.setState("MO");
			}
		}
		return count;
	}

	private void populateSnapshot(long currentSnapshotId, SnapshotManager snapshotManager,
			Map<Long, List<DbDeviceOutageReader>> transformerMap) {
		System.out.println("Populating snapshot ID: " + currentSnapshotId);
		
		SnapshotPayload previousSnapshotPayload = snapshotManager.getSnapshot(currentSnapshotId-1);
		SnapshotPayload currentSnapshot = snapshotManager.getSnapshot(currentSnapshotId);
		
		for(StatePayload state: currentSnapshot.getStates()) {
			//find all the order numbers in current snapshot
			Set<String> currentSnapshotOrderNumbers = new HashSet<>();
			for(OrderPayload orderPayload: state.getOrders()) {
				currentSnapshotOrderNumbers.add(orderPayload.getOrderNumber());
			}
			
			//add previous orders which are not present in current
			if(previousSnapshotPayload != null) {
				List<OrderPayload> previousOrders = previousSnapshotPayload.getState(state.getState()).getOrders();
				for(OrderPayload order: previousOrders) {
					if(!currentSnapshotOrderNumbers.contains(order.getOrderNumber())) {
						order.setTransformers(null);
						order.setPremiseNumber(null);
						state.getOrders().add(order);
					}

				}
			}
			
			//go over each order
			Set<OrderPayload> toBeRemoved = new HashSet<>();
			for(OrderPayload orderPayload: state.getOrders()) {
//				List<DbDeviceOutageReader> transformersByOrderId = transformerMap.get(orderPayload.getId());
//				if(transformersByOrderId != null && !transformersByOrderId.isEmpty()) {
//					orderPayload.setDbDeviceOutageReaderList(transformersByOrderId);
					orderPayload.populateTransformers(currentSnapshotId);
//				}else {
//					System.out.println("Initial Order Detail does not have transformers: " + orderPayload.getId());
////					secondTry(currentSnapshotId, orderPayload, snapshotManager);
//				}
				if(orderPayload.getTransformers() == null || orderPayload.getTransformers().isEmpty()) {
					toBeRemoved.add(orderPayload);
				}
			}
			
			if(!toBeRemoved.isEmpty()) {
				state.getOrders().removeAll(toBeRemoved);
			}
			
			
			StringBuffer sb = new StringBuffer();
			for(OrderPayload order :state.getOrders()) {
				sb.append(order.getOrderNumber()).append(",");
			}
			System.out.println("Orders in the payload: " + sb.toString());
		}
	}
	
	
/*	
	private void populateSnapshot(long currentSnapshotId, SnapshotManager snapshotManager,
			Map<Long, List<DbDeviceOutageReader>> transformerMap) {
		System.out.println("Current snapshot ID: " + currentSnapshotId);
		
		SnapshotPayload previousSnapshotPayload = snapshotManager.getSnapshot(currentSnapshotId-1);
		SnapshotPayload currentSnapshot = snapshotManager.getSnapshot(currentSnapshotId);
		
		for(StatePayload state: currentSnapshot.getStates()) {
			List<OrderPayload> orders = state.getOrders();
			//add previous orders
			if(previousSnapshotPayload != null) {
				List<OrderPayload> previousOrders = previousSnapshotPayload.getState(state.getState()).getOrders();
				for(OrderPayload order: previousOrders) {
					order.setTransformers(null);
					order.setPremiseNumber(null);
				}
				orders.addAll(previousOrders);
			}
			//go over each order
			Set<OrderPayload> toBeRemoved = new HashSet<>();
			for(OrderPayload orderPayload: state.getOrders()) {
				List<DbDeviceOutageReader> transformersByOrderId = transformerMap.get(orderPayload.getId());
				if(transformersByOrderId != null && !transformersByOrderId.isEmpty()) {
					orderPayload.setDbDeviceOutageReaderList(transformersByOrderId);
					orderPayload.populateTransformers(currentSnapshotId);
				}else {
					System.out.println("Initial Order Detail does not have transformers: " + orderPayload.getId());
//					secondTry(currentSnapshotId, orderPayload, snapshotManager);
				}
				if(orderPayload.getTransformers() == null || orderPayload.getTransformers().isEmpty()) {
					toBeRemoved.add(orderPayload);
				}
			}
			state.getOrders().removeAll(toBeRemoved);
			
			StringBuffer sb = new StringBuffer();
			for(OrderPayload order :state.getOrders()) {
				sb.append(order.getOrderNumber()).append(",");
			}
			System.out.println("Orders in the payload: " + sb.toString());
		}
	}
*/
//	private void secondTry(long currentSnapshotId, OrderPayload orderPayload, SnapshotManager snapshotManager) {
//		List<DbDeviceOutageReader> list = transformerFirstSnapshotMap.get(currentSnapshotId);
//		for(DbDeviceOutageReader device: list) {
//			snapshotManager.getSnapshots();
//		}
//	}

	private void populateDevices(Map<Long, List<DbDeviceOutageReader>> transformerMap, OrderPayload orderPayload, long id) {
		List<DbDeviceOutageReader> transformersByOrderId = transformerMap.get(id);
		if(transformersByOrderId != null && !transformersByOrderId.isEmpty()) {
			String premiseNumber = null;
			boolean premiseFound = false;
			List<String> transformers = new ArrayList<>();
			for(DbDeviceOutageReader transformer: transformersByOrderId) {
				transformers.add(transformer.getTransformer_number());
				if(transformer.getPremise_number() != null) {
					if(premiseFound) {
						System.out.println("Error: multiple premise found: " + id);
					}else {
						premiseNumber = transformer.getPremise_number();
						premiseFound = true;	
					}
				}
			}
			orderPayload.setPremiseNumber(premiseNumber);
			orderPayload.setTransformers(transformers);
		}
	}

	
	
	
	private static Map<Long, List<DbDeviceOutageReader>> transformerFirstSnapshotMap = new HashMap<>();
	private static Map<String, List<DbDeviceOutageReader>> transformerByOrderNumberMap = new HashMap<>();
	
	private Map<Long, List<DbDeviceOutageReader>> prepareTransformers() {
		//order detail id <-> list of transformers
		Map<Long, List<DbDeviceOutageReader>> transformers =new HashMap<>();
		
		try {
			String json = new String(
//					Files.readAllBytes(new File("src/main/resources/TestT.json").toPath()));
					Files.readAllBytes(new File(DEVICE_FILE).toPath()));
//			System.out.println("number of order details: " + dbTransformers.getTransformers().size());
			List<DbDeviceOutageReader> dbTransformerList = mapper.readValue(json, new TypeReference<List<DbDeviceOutageReader>>(){});
			System.out.println("number of device records: " + dbTransformerList.size());
//			System.out.println(mapper.writeValueAsString(OrderConverter.convert( dbOrderList.getOrders().get(0))));

			for(DbDeviceOutageReader dbTransformer : dbTransformerList) {
				List<DbDeviceOutageReader> list = transformers.get(dbTransformer.getOrder_detail_id());
				if(list == null) {
					list = new ArrayList<>();
					transformers.put(dbTransformer.getOrder_detail_id(), list);
				}
				list.add(dbTransformer);
				
				String orderNumber = orderIdNumberMap.get(dbTransformer.getOrder_detail_id());
				if(orderNumber != null) {
					for(OrderPayload orderPayload : orderNumber_OrderList.get(orderNumber)) {
						if(orderPayload.getSnapshotId()<= dbTransformer.getUpdated_snapshot_id()) {
							orderPayload.addDbDeviceOutageReader(dbTransformer);
						}
					}
					
				}else {
					System.out.println("There is no order found for id: " + dbTransformer.getOrder_detail_id());
				}
				//special case:
//				if(dbTransformer.getFirst_snapshot_id() != dbTransformer.getUpdated_snapshot_id()) {
//					mapTransformerFirstSnapshotId(transformerFirstSnapshotMap, dbTransformer);
//				}
				
			}

			checkOrderDevices();
			return transformers;
		} catch (Exception e) {
			System.out.println("failed to load");
			e.printStackTrace();
			return null;
		}
//		return TransactionService;
	}

	private void checkOrderDevices() {
//		int count = 0;
//		StringBuffer sb = new StringBuffer();
		Set<Long> orderIds = new HashSet<>();
		for(List<OrderPayload> orders: orderNumber_OrderList.values()) {
			for(OrderPayload order:orders) {
				if(order.getDevices().isEmpty()) {
//					sb.append(order.getId()).append(",");
					orderIds.add(order.getId());
//					count++;
				}
			}
		}
		if(orderIds.size() > 0) {
			List<Long> sortedOrderids = new ArrayList(orderIds);
			Collections.sort(sortedOrderids);
			StringBuffer sb = new StringBuffer();
			for(Long id: sortedOrderids) {
				sb.append(id).append(",");
			}
			System.out.println("There are " + orderIds.size() + " order details without transformers: " + sb.toString() );
		}
	}

	//Map of snapshotID <-> Map<orderNumber <-> List<transformer>>
	private void mapTransformerFirstSnapshotId(Map<Long, List<DbDeviceOutageReader>> map,
			DbDeviceOutageReader dbTransformer) {
		List<DbDeviceOutageReader> list = map.get(dbTransformer.getFirst_snapshot_id());
		if (list == null) {
			list = new ArrayList<>();
			map.put(dbTransformer.getFirst_snapshot_id(), list);
		}
		list.add(dbTransformer);
	}

	public void setAutoRun(boolean autoRun) {
		this.autoRun = autoRun;
	}

	public void setStartFrom(int count) {
		runCount = count;
	}

}
