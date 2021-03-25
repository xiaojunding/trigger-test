package com.ameren.trigger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ameren.outage.core.model.Outage;

@RestController
public class ManualTriggerController {

	@Autowired
//	private RestTemplate restTemplate;
//	private String getSnapshotUrlString = "https://hawscorpt.ameren.com:8443/sys/eadms/outage/v1/snapshot";
//	private String postOutageToOchUrlString = "https://outage-dev.ameren.com/snapshot/process";
	private Job worker;
	@RequestMapping(value = "/manualTrigger", method = RequestMethod.GET)
	public void manulaTrigger() {
		System.out.println("manual trigger");
		worker.doTheJob();
//		HttpHeaders headers = new HttpHeaders();
//	    headers.setContentType(MediaType.APPLICATION_JSON);
//	    headers.add("client_id", "7eddf5ecc4a9459dbf569a04236bed43");
//	    headers.add("client_secret", "facc07B5a71C4df380b69f6cb3614C05");
//	    
//		HttpEntity requestEntity = new HttpEntity(headers);
//		ResponseEntity<String> snapshot = restTemplate.exchange(getSnapshotUrlString, HttpMethod.GET, requestEntity , String.class);
//		String body = snapshot.getBody();
//		
//		int start = body.indexOf('{', 5);
//		int end = body.lastIndexOf('}');
//		String postPayload = body.substring(start, end);
//		System.out.println(postPayload);
//		HttpEntity<String> postEntity = new HttpEntity<String>(postPayload, headers);
//		ResponseEntity<String> responseEntity = restTemplate.exchange(postOutageToOchUrlString, HttpMethod.POST, postEntity, String.class);
//		System.out.println(responseEntity.getBody());
	}
	
	@RequestMapping(value = "/analyze", method = RequestMethod.GET, produces="text/plain")
		public String analyze(@RequestParam(required = false, defaultValue = "prod", name = "env") String env) {
		System.out.println("Analyzing Outages");
		Outage outage = worker.getSnapshot(env);
		
		StringBuilder analyzeResult = worker.analyze(outage);
		return analyzeResult.toString(); 
	}
	
	@RequestMapping(value = "/sendQA", method = RequestMethod.GET, produces="text/plain")
	public String getFromQA_SentToQA() {
		System.out.println("Manual sending payload to QA");
		worker.doTheJob("https://outage-qa.ameren.com/snapshot/process");
		return "Done"; 
	}
	
	@RequestMapping(value = "/setup", method = RequestMethod.GET, produces="text/plain")
	public String setupLoadTest() {
		System.out.println("Start preparing load test data");
		worker.prepareLoadTestData();
		return "Done"; 
	}
	
	@RequestMapping(value = "/get", method = RequestMethod.GET, produces="text/plain")
	public String getPayload(@RequestParam(required = false, name="count") Integer count) {
		if(count != null && count > 0) {
			worker.getPayload(count);
		}
		return "Done"; 
	}
	
	@RequestMapping(value = "/start", method = RequestMethod.GET, produces="text/plain")
	public String startLoadTest(@RequestParam(required = false, name="count") Integer count) {
		if(count != null && count > 0) {
			worker.setStartFrom(count);
		}
		worker.setAutoRun(true);
		return "Done"; 
	}
	
	@RequestMapping(value = "/stop", method = RequestMethod.GET, produces="text/plain")
	public String stopLoadTest() {
		worker.setAutoRun(false);
		return "Done"; 
	}
	
}
