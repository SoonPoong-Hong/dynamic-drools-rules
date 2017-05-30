package org.ossandme;

import org.drools.template.ObjectDataCompiler;
import org.kie.api.KieServices;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.ossandme.rule.Condition;
import org.ossandme.rule.Rule;
import org.ossandme.event.Event;
import org.ossandme.event.OrderEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class Program2 {

	static public void main(String[] args) throws Exception {
		Program2 main = new Program2();
		main.init();
		for (int i = 0; i < 10; i++) {
			main.process();
		}
	}

	StatelessKieSession statelessKieSession;

	private void init() throws Exception {
		long start = System.currentTimeMillis();
		Rule highValueOrderWidgetsIncRule = new Rule();

		Condition highValueOrderCondition = new Condition();
		highValueOrderCondition.setField("price");
		highValueOrderCondition.setOperator(Condition.Operator.GREATER_THAN);
		highValueOrderCondition.setValue(5000.0);

		Condition widgetsIncCustomerCondition = new Condition();
		widgetsIncCustomerCondition.setField("customer");
		widgetsIncCustomerCondition.setOperator(Condition.Operator.EQUAL_TO);
		widgetsIncCustomerCondition.setValue("Widgets Inc.");

		// In reality, you would have multiple rules for different types of
		// events.
		// The eventType property would be used to find rules relevant to the
		// event
		highValueOrderWidgetsIncRule.setEventType(Rule.eventType.ORDER);

		highValueOrderWidgetsIncRule.setConditions(Arrays.asList(highValueOrderCondition, widgetsIncCustomerCondition));

		drl = applyRuleTemplate(new OrderEvent(), highValueOrderWidgetsIncRule);
		System.out.println("== drl : \n" + drl);
		System.out.println("==============");

		KieServices kieServices = KieServices.Factory.get();
		KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
		// kieFileSystem.write("src/main/resources/rule.drl", drl);
		//=== 별거 없다. drl의 string을 새로 만들어서, 아래와 같이 write한후에 빌드하면 된다.
		kieFileSystem.write("src/main/resources/r1.drl", drl);
		kieServices.newKieBuilder(kieFileSystem).buildAll();

		KieContainer kieContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
		statelessKieSession = kieContainer.getKieBase().newStatelessKieSession();
		long end = System.currentTimeMillis();
		System.out.println("== init : " + (end-start));
	}

	String drl;

	void process() throws Exception {
		// Create an event that will be tested against the rule. In reality, the
		// event would be read from some external store.
		OrderEvent orderEvent = new OrderEvent();
		orderEvent.setPrice(5000.1);
		orderEvent.setCustomer("Widgets Inc.");

		AlertDecision alertDecision = evaluate(drl, orderEvent);

		System.out.println(alertDecision.getDoAlert());

		// doAlert is false by default
		if (alertDecision.getDoAlert()) {
			// do notification
		}
	}

	private AlertDecision evaluate(String drl, Event event) throws Exception {
		long start = System.currentTimeMillis();
		AlertDecision alertDecision = new AlertDecision();
		statelessKieSession.getGlobals().set("alertDecision", alertDecision);
		statelessKieSession.execute(event);
		long end = System.currentTimeMillis();
		System.out.println("== evaludate : " + (end - start));
		return alertDecision;
	}

	private String applyRuleTemplate(Event event, Rule rule) throws Exception {
		Map<String, Object> data = new HashMap<String, Object>();
		ObjectDataCompiler objectDataCompiler = new ObjectDataCompiler();

		data.put("rule", rule);
		data.put("eventType", event.getClass().getName());

		return objectDataCompiler.compile(Arrays.asList(data),
				Thread.currentThread().getContextClassLoader().getResourceAsStream("rule-template.drl"));
	}
}
