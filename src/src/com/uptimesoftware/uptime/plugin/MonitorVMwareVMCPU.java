package com.uptimesoftware.uptime.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.fortsoft.pf4j.PluginWrapper;
import com.uptimesoftware.uptime.plugin.api.Extension;
import com.uptimesoftware.uptime.plugin.api.Plugin;
import com.uptimesoftware.uptime.plugin.api.PluginMonitor;
import com.uptimesoftware.uptime.plugin.monitor.MonitorState;
import com.uptimesoftware.uptime.plugin.monitor.Parameters;

/**
 * MonitorVMwareVMCPU
 * 
 * @author uptime software
 */
public class MonitorVMwareVMCPU extends Plugin {

	/**
	 * Constructor - a plugin wrapper.
	 * 
	 * @param wrapper
	 */
	public MonitorVMwareVMCPU(PluginWrapper wrapper) {
		super(wrapper);
	}

	/**
	 * A nested static class which has to extend PluginMonitor.
	 * 
	 * Functions that require implementation :
	 * 1) The monitor function will implement the main functionality and should set the monitor's state and result
	 * message prior to completion.
	 * 2) The setParameters function will accept a Parameters object containing the values filled into the monitor's
	 * configuration page in Up.time.
	 */
	@Extension
	public static class UptimeMonitorVMwareVMCPU extends PluginMonitor {
		// Logger object.
		private static final Logger LOGGER = LoggerFactory.getLogger(UptimeMonitorVMwareVMCPU.class);

		// See definition in .xml file for plugin. Each plugin has different number of input/output parameters.
		// [Input]
		String hostname = "";

		/**
		 * The setParameters function will accept a Parameters object containing the values filled into the monitor's
		 * configuration page in Up.time.
		 * 
		 * @param params
		 *            Parameters object which contains inputs.
		 */
		@Override
		public void setParameters(Parameters params) {
			LOGGER.debug("Step 1 : Setting parameters.");
			// [Input]
			hostname = params.getString("hostname");
		}

		/**
		 * The monitor function will implement the main functionality and should set the monitor's state and result
		 * message prior to completion.
		 */
		@Override
		public void monitor() {
			LOGGER.debug("Connect to up.time Data Store and execute the sql query.");
			String sqlQuery = "SELECT sample_time, ready, wait, pmem.ballooned FROM vmware_perf_vm_cpu pvc "
					+ "INNER JOIN vmware_perf_sample ps ON pvc.sample_id = ps.sample_id	"
					+ "INNER JOIN vmware_object vo ON ps.vmware_object_id = vo.vmware_object_id "
					+ "INNER JOIN entity e ON e.entity_id = vo.entity_id "
					+ "INNER JOIN vmware_perf_mem pmem ON pmem.sample_id = ps.sample_id	"
					+ "WHERE ps.sample_time > DATE_SUB(now(), INTERVAL 15 minute) AND e.name =?"
					+ " AND e.display_name = vo.display_name ORDER BY sample_time DESC LIMIT 1";

			List<Object> variables = new ArrayList<Object>();
			variables.add(hostname);

			List<HashMap<String, Object>> result = new ArrayList<HashMap<String, Object>>();
			result = executeQuery(sqlQuery, variables);

			LOGGER.debug("Check the size of result List");
			if (result.size() == 0) {
				setStateAndMessage(MonitorState.CRIT, "Failed to retrieve result.");
				return;
			}

			for (int i = 0; i < result.size(); i++) {
				for (Entry<String, Object> entry : result.get(i).entrySet()) {
					// entry.getValue() returns Long.
					if (entry.getKey().contains("wait")) {
						addVariable("pctWait", entry.getValue().toString());
					} else if (entry.getKey().contains("ready")) {
						addVariable("pctReady", entry.getValue().toString());
					} else if (entry.getKey().contains("ballooned")) {
						addVariable("memBalloon", entry.getValue().toString());
					}
				}
			}

			LOGGER.debug("Monitor ran successfully.");
			setStateAndMessage(MonitorState.OK, "Monitor ran successfully.");
		}
	}
}