package org.icatproject.ijp_portal.client.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.icatproject.ijp_portal.shared.AccountDTO;
import org.icatproject.ijp_portal.shared.DatasetOverview;
import org.icatproject.ijp_portal.shared.GenericSearchSelections;
import org.icatproject.ijp_portal.shared.InternalException;
import org.icatproject.ijp_portal.shared.JobDTO;
import org.icatproject.ijp_portal.shared.ParameterException;
import org.icatproject.ijp_portal.shared.ServerException;
import org.icatproject.ijp_portal.shared.SessionException;
import org.icatproject.ijp_portal.shared.PortalUtils.MultiJobTypes;
import org.icatproject.ijp_portal.shared.PortalUtils.OutputType;
import org.icatproject.ijp_portal.shared.PortalUtils.ParameterValueType;
import org.icatproject.ijp_portal.shared.xmlmodel.JobType;
import org.icatproject.ijp_portal.shared.xmlmodel.JobTypeMappings;
import org.icatproject.ijp_portal.shared.xmlmodel.SearchItems;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface DataServiceAsync {
	void getDatasetList(String sessionId, String datasetType,
			Map<String, List<String>> selectedSearchParamsMap,
			List<GenericSearchSelections> genericSearchSelectionsList,
			AsyncCallback<List<DatasetOverview>> callback);

	void getDatasetParameters(String sessionId, Long datasetId,
			AsyncCallback<LinkedHashMap<String, String>> callback);

	void login(String plugin, Map<String, String> credentials, AsyncCallback<String> callback);

	void getJobsForUser(String sessionId, AsyncCallback<List<JobDTO>> callback);

	void getJobOutput(String sessionId, String jobId, OutputType outputType,
			AsyncCallback<String> callback);

	void getSearchItems(AsyncCallback<SearchItems> callback);

	void getJobTypeMappings(AsyncCallback<JobTypeMappings> callback);

	void getDatasetTypesList(String sessionId, AsyncCallback<List<String>> callback);

//	void getAccountFor(String sessionId, String command, String parameters,
//			AsyncCallback<AccountDTO> callback);

	void getDatasetParameterTypesMap(String sessionId,
			AsyncCallback<LinkedHashMap<String, ParameterValueType>> callback);

	void getJobDatasetParametersForDatasets(String sessionId,
			String datasetType, List<Long> datasetIds,
			AsyncCallback<Map<Long, Map<String, Object>>> callback);

//	void submitBatchFromPortal(String sessionId, String jobName, String options,
//			String datasetIds, MultiJobTypes multiJobType,
//			AsyncCallback<String> callback);
	void submitBatchFromPortal(String sessionId, JobType jobType, List<String> parameters, AsyncCallback<String> callback);

//	void submitInteractiveFromPortal(String sessionId, String jobName,
//			String options, String datasetIds,
//			AsyncCallback<AccountDTO> callback);
	void submitInteractive(String sessionId, JobType jobType,
			List<String> parameters, AsyncCallback<AccountDTO> callback);
			
	void addDoubleToSerializationPolicy(Double aDouble, AsyncCallback<Double> callback);

}
