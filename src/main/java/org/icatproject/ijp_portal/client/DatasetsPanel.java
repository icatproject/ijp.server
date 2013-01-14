package org.icatproject.ijp_portal.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.icatproject.ijp_portal.client.service.DataService;
import org.icatproject.ijp_portal.client.service.DataServiceAsync;

import org.icatproject.ijp_portal.shared.DatasetOverview;
import org.icatproject.ijp_portal.shared.PortalUtils;
import org.icatproject.ijp_portal.shared.ServerException;
import org.icatproject.ijp_portal.shared.SessionException;
import org.icatproject.ijp_portal.shared.xmlmodel.JobType;
import org.icatproject.ijp_portal.shared.xmlmodel.JobTypeMappings;
import org.icatproject.ijp_portal.shared.xmlmodel.ListOption;
import org.icatproject.ijp_portal.shared.xmlmodel.SearchItem;
import org.icatproject.ijp_portal.shared.xmlmodel.SearchItems;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

public class DatasetsPanel extends Composite implements RequiresResize {
	// Annotation can be used to change the name of the associated xml file
	// @UiTemplate("LoginPanel.ui.xml")
	interface MyUiBinder extends UiBinder<Widget, DatasetsPanel> {
	}

	@UiField
	ListBox datasetTypeListBox;
	
	@UiField
	FlowPanel searchListsPanel;

	@UiField
	Button searchButton;

	@UiField
	Button doStuffButton;

	@UiField
	TextArea debugTextArea;
	
	@UiField
	Label messageLabel;
	
	@UiField
	CellTable<DatasetOverview> datasetsTable;
	
	@UiField
	ListBox datasetActionListBox;

	@UiField
	Button datasetInfoButton;

	@UiField
	CellTable<DatasetInfoItem> datasetInfoTable;

	@UiField
	ScrollPanel datasetsScrollPanel;

	@UiField
	ScrollPanel infoScrollPanel;

	@UiField
	FormPanel rdpForm;
	@UiField
	Hidden hostNameField;
	@UiField
	Hidden accountNameField;

	@UiField
	FormPanel downloadForm;
	@UiField
	Hidden sessionIdField;
	@UiField
	Hidden datasetIdField;
	@UiField
	Hidden datasetNameField;

	private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
	private DataServiceAsync dataService = GWT.create(DataService.class);

	private List<DatasetOverview> datasetList = new ArrayList<DatasetOverview>();
    final SingleSelectionModel<DatasetOverview> selectionModel = new SingleSelectionModel<DatasetOverview>();

    private static final String OPTIONS_LIST_FIRST_OPTION = "Options ...";
    private static final String DEFAULT_MESSAGE = "Select a Dataset Type and do a Search";
    
    Portal portal;
    Map<String, ListBox> searchItemsListBoxMap = new HashMap<String, ListBox>();
    JobTypeMappings jobTypeMappings;
    
	public DatasetsPanel(Portal portal) {
		this.portal = portal;
		initWidget(uiBinder.createAndBindUi(this));

		datasetActionListBox.addItem(OPTIONS_LIST_FIRST_OPTION);
		datasetActionListBox.setEnabled(false);
		
//		datasetsTable.setPageSize(5);
//		datasetsScrollPanel.setAlwaysShowScrollBars(true);
		
		searchButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
			    refreshDatasetsList();
			}
		});

		doStuffButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				displayJobTypesInTextArea();
			}
		});

		datasetInfoButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
		        DatasetOverview selectedDataset = selectionModel.getSelectedObject();
		        if (selectedDataset != null) {
		        	String message = "";
		        	message += "ID: "+ selectedDataset.getDatasetId() + "\n";
		        	Map<String, Object> jobDatasetParameters = selectedDataset.getJobDatasetParameters();
		        	for (String key : jobDatasetParameters.keySet() ) {
		        		Object dsParamObject = jobDatasetParameters.get(key);
		        		String dsParamAsString = "null";
		        		if ( dsParamObject != null ) {
		        			dsParamAsString = dsParamObject.toString();
		        		}
			        	message += "JobDsParam: " + key + ": "+ dsParamAsString + " ";
		        		if ( dsParamObject != null ) {
		        			message += "Class: " + jobDatasetParameters.get(key).getClass().getName();
		        		}
		        		message += "\n";
		        	}
		            Window.alert(message);
		        } else {
		            Window.alert("Please select a dataset");
		        }
			}
		});

		// Add a text column to show the name.
	    TextColumn<DatasetOverview> nameColumn = new TextColumn<DatasetOverview>() {
	      @Override
	      public String getValue(DatasetOverview dataset) {
	        return dataset.getName();
	      }
	    };
	    datasetsTable.addColumn(nameColumn, "Name");

	    // Add a text column to show the sample description.
	    TextColumn<DatasetOverview> sampleDescriptionColumn = new TextColumn<DatasetOverview>() {
	      @Override
	      public String getValue(DatasetOverview dataset) {
	        return dataset.getSampleDescription();
	      }
	    };
	    datasetsTable.addColumn(sampleDescriptionColumn, "Sample Description");
	    
	    // Add a text column to show the directory.
	    TextColumn<DatasetOverview> usersColumn = new TextColumn<DatasetOverview>() {
	      @Override
	      public String getValue(DatasetOverview dataset) {
	        return dataset.getUsers();
	      }
	    };
	    datasetsTable.addColumn(usersColumn, "Users");
	    
	    // configure the selection model to handle user selection of datasets
	    datasetsTable.setSelectionModel(selectionModel);
	    selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
	      public void onSelectionChange(SelectionChangeEvent event) {
	        DatasetOverview selectedDataset = selectionModel.getSelectedObject();
	        if (selectedDataset != null) {
	        	refreshDatasetInformation(selectedDataset.getDatasetId());
//	        	updateOptionsListBox(selectedDataset);
	        }
	      }
	    });

	    // Add a text column to show the name.
	    TextColumn<DatasetInfoItem> infoNameColumn = new TextColumn<DatasetInfoItem>() {
	      @Override
	      public String getValue(DatasetInfoItem datasetInfoItem) {
	        return datasetInfoItem.getName();
	      }
	    };
	    datasetInfoTable.addColumn(infoNameColumn);

	    // Add a text column to show the value.
	    TextColumn<DatasetInfoItem> infoValueColumn = new TextColumn<DatasetInfoItem>() {
	      @Override
	      public String getValue(DatasetInfoItem datasetInfoItem) {
	        return datasetInfoItem.getValue();
	      }
	    };
	    datasetInfoTable.addColumn(infoValueColumn);

	    // 2 lines below needed to get rid of the 3 flashing "progress indicator" boxes
	    datasetsTable.setRowCount(0);
	    datasetInfoTable.setRowCount(0);
	    
	    messageLabel.setText(DEFAULT_MESSAGE);
	    
	    // for the rdpForm to request an rdp file download
	    rdpForm.setEncoding(FormPanel.ENCODING_URLENCODED);
		rdpForm.setMethod(FormPanel.METHOD_GET);
		rdpForm.setAction(GWT.getHostPageBaseURL() + "rdp");

	    // for the downloadForm to request a dataset zip file download
	    downloadForm.setEncoding(FormPanel.ENCODING_URLENCODED);
		downloadForm.setMethod(FormPanel.METHOD_GET);
		downloadForm.setAction(GWT.getHostPageBaseURL() + "download");

		addSearchBoxesAndPopulateTextArea();

		// TODO - remove these later - just for development purposes
    	doStuffButton.setVisible(false);
		debugTextArea.setVisible(false);
    	
		System.out.println("DatasetsPanel: constructor complete");
	}
	
	@UiHandler("datasetTypeListBox")
	void handleDatasetTypeListBoxChange(ChangeEvent event) {
		String selectedValue = datasetTypeListBox.getValue(datasetTypeListBox.getSelectedIndex());
//		Window.alert("Value is:'" + selectedValue + "'");
		datasetActionListBox.clear();
		datasetActionListBox.addItem(OPTIONS_LIST_FIRST_OPTION, "");

		// remove any datasets listed in the Datasets Table
		datasetList = new ArrayList<DatasetOverview>();
		datasetsTable.setRowData(datasetList);
		datasetsTable.setRowCount(0);
		// remove any data displayed in the Dataset Information area
    	List<DatasetInfoItem> infoItemList = new ArrayList<DatasetInfoItem>();
		datasetInfoTable.setRowData(infoItemList);
		datasetInfoTable.setRowCount(0);
		// disable the dataset actions box - it will be re-enabled when a search
		// is done and a new list of datasets appears
		datasetActionListBox.setEnabled(false);
		// remove any message text ("3 datasets found" etc)
		messageLabel.setText(DEFAULT_MESSAGE);
		
		// the value of the first option "Dataset types..." is an empty string
		// for this option we don't need to add any job options
		if ( !selectedValue.equals("") ) {
			for (String jobName : jobTypeMappings.getJobTypesMap().keySet()) {
				JobType jobType = jobTypeMappings.getJobTypesMap().get(jobName);
				if (jobType.getDatasetTypes().contains(selectedValue)) {
					datasetActionListBox.addItem(jobType.getName());
				}
			}
		}
	}

	@UiHandler("datasetActionListBox")
	void handleDatasetActionListBoxChange(ChangeEvent event) {
		String jobName = datasetActionListBox.getValue(datasetActionListBox.getSelectedIndex());
//		Window.alert("jobName is:'" + jobName + "'");
		if ( !jobName.equals("") ) {
			// popup a form containing the options for this job
			// with options relevant to the selected dataset
			portal.jobOptionsPanel.populateAndShowForm(jobName);
		}
	}
	
	void populateDatasetTypeListBox() {
		dataService.getDatasetTypesList(portal.getSessionId(), new AsyncCallback<List<String>>() {
			@Override
			public void onFailure(Throwable caught) {
				Window.alert("Server error: " + caught.getMessage());
			}
	
			@Override
			public void onSuccess(List<String> datasetTypesList) {
				for (String datasetType : datasetTypesList) {
					datasetTypeListBox.addItem(datasetType);
				}
			}
		});
	}

	protected void getJobTypesFromServer() {
		// put the JobTypes looked up from XML on the server into a variable
		dataService.getJobTypeMappings(new AsyncCallback<JobTypeMappings>() {
			@Override
			public void onFailure(Throwable caught) {
				Window.alert("Server error: " + caught.getMessage());
			}
	
			@Override
			public void onSuccess(JobTypeMappings jobTypeMappings) {
				setJobTypeMappings(jobTypeMappings);
			}
		});
	}
	
	
	protected void displayJobTypesInTextArea() {
		if ( jobTypeMappings == null ) {
			debugTextArea.setText("jobTypeMappings is null");
		} else {
			debugTextArea.setText(jobTypeMappings.toString());
		}
	}
	
	private void setJobTypeMappings(JobTypeMappings jobTypeMappings) {
		this.jobTypeMappings = jobTypeMappings;
	}
	
	protected void addSearchBoxesAndPopulateTextArea() {
		// put the SearchItems looked up from XML into a TextArea
		dataService.getSearchItems(new AsyncCallback<SearchItems>() {
			@Override
			public void onFailure(Throwable caught) {
				Window.alert("Server error: " + caught.getMessage());
			}
	
			@Override
			public void onSuccess(SearchItems searchItems) {
				if ( searchItems == null ) {
					debugTextArea.setText("searchItems is null");
				} else {
					debugTextArea.setText(searchItems.toString());
					for ( SearchItem searchItem : searchItems.getSearchItemList() ) {
						ListBox listBox = new ListBox(searchItem.isMultipleSelect());
//						listBox.setName(searchItem.paramName);
						for ( ListOption listOption : searchItem.getListOptions() ) {
							listBox.addItem(listOption.getDisplayValue(), listOption.getSubmitValue());
						}
						listBox.setVisibleItemCount(searchItem.getVisibleItemCount());
						listBox.setSelectedIndex(0);
						searchListsPanel.add(listBox);
						searchItemsListBoxMap.put(searchItem.getParamName(), listBox);
					}
				}
			}
		});
	}

	private void refreshDatasetInformation(Long datasetId) {
		AsyncCallback<LinkedHashMap<String,String>> callback = new AsyncCallback<LinkedHashMap<String,String>>() {
	    	public void onFailure(Throwable caught) {
	    		// deal with possible exceptions
	    		System.err.println("DatasetsPanel.refreshDatasetInformation(): " + caught.getMessage());
	    		if ( caught.getClass() == SessionException.class ) {
	    			System.err.println("caught is a SessionException");
	    			portal.loginPanel.setMessageText(caught.getMessage());
	    			portal.loginDialog.show();
	    		} else if ( caught.getClass() == ServerException.class ) {
		            Window.alert("Server error: " + caught.getMessage());
	    		} else {
	    			// no other exceptions are expected
	    		}
	    	}

	    	public void onSuccess(LinkedHashMap<String,String> result) {
	    		Set<String> paramNames = result.keySet();
	    		Iterator<String> paramNamesIterator = paramNames.iterator();
	        	List<DatasetInfoItem> infoItemList = new ArrayList<DatasetInfoItem>();
	    		while ( paramNamesIterator.hasNext() ) {
	    			String paramName = paramNamesIterator.next();
		        	infoItemList.add( new DatasetInfoItem(paramName,result.get(paramName)) );
	    		}
	    		datasetInfoTable.setRowData(infoItemList);
	    	}
		};
		// make the call to the server
		System.out.println("DatasetsPanel: making call to DataService");
		dataService.getDatasetParameters(portal.getSessionId(), datasetId, callback);
	}

	protected void refreshDatasetsList() {
		String selectedDatasetType = null;
		if ( datasetTypeListBox.getSelectedIndex() == 0 ) {
            Window.alert("Please select a dataset type");
            return;
		} else {
			selectedDatasetType = datasetTypeListBox.getValue(datasetTypeListBox.getSelectedIndex());
		}
		
	    // set up the callback object
		AsyncCallback<List<DatasetOverview>> callback = new AsyncCallback<List<DatasetOverview>>() {
	    	public void onFailure(Throwable caught) {
	    		// deal with possible exceptions
	    		System.err.println("DatasetsPanel.refreshDatasetsList(): " + caught.getMessage());
	    		if ( caught.getClass() == SessionException.class ) {
	    			System.err.println("caught is a SessionException");
	    			portal.loginPanel.setMessageText(caught.getMessage());
	    			portal.loginDialog.show();
	    		} else if ( caught.getClass() == ServerException.class ) {
		            Window.alert("Server error: " + caught.getMessage());
	    		} else {
	    			// no other exceptions are expected
	    		}
	    	}

	    	public void onSuccess(List<DatasetOverview> result) {
	    		if ( result == null ) {
	    			System.out.println("Result is null");
	    		} else {
	    			int resultSize = result.size();
	    			System.out.println("Result size: " + resultSize);
	    			if ( resultSize == PortalUtils.MAX_RESULTS ) {
	    				// TODO - get the colour of the message changing to work
	    				messageLabel.setText("Results limit of " + PortalUtils.MAX_RESULTS + " reached. Please refine your search.");
	    			} else {
    					messageLabel.setText(resultSize + " datasets found.");
	    			}
	    		}
	    		datasetList = result;
	    		// set the page size to the number of datasets returned
	    		// otherwise we only see the first 15 by default
	    		datasetsTable.setPageSize(datasetList.size());
	    	    // Set the total row count. This isn't strictly necessary, but it affects
	    	    // paging calculations, so its good habit to keep the row count up to date
	    		// KP - this doesn't seem to work ??
//	    	    datasetsTable.setRowCount(datasetList.size(), true);
	    	    // Push the data into the widget.
	    	    datasetsTable.setRowData(0, datasetList);
	    	    if ( datasetList.size() == 0 ) {
	    	    	// remove any data displayed in the Dataset Information area
		        	List<DatasetInfoItem> infoItemList = new ArrayList<DatasetInfoItem>();
		        	// TODO - should I show a list of parameter names with blank values???
		    		datasetInfoTable.setRowData(infoItemList);
	    	    	datasetActionListBox.setEnabled(false);
	    	    } else {
		    	    // set the first item in the list to selected 
		    	    // so that the DatasetOverview information pane gets populated
	    	    	selectionModel.setSelected(datasetList.get(0), true);
	    	    	datasetActionListBox.setEnabled(true);
	    	    }
	    	}
		};

		// make the call to the server
		System.out.println("DatasetsPanel: making call to DataService");
		dataService.getDatasetList(portal.getSessionId(), selectedDatasetType, getSearchParamsMap(), callback);
	}

	private Map<String, List<String>> getSearchParamsMap() {
		Map<String, List<String>> searchParamsMap = new HashMap<String, List<String>>();
		for ( String key : searchItemsListBoxMap.keySet() ) {
			List<String> selectedItemsList = new ArrayList<String>();
			ListBox listBox = searchItemsListBoxMap.get(key);
			for (int i=0; i<listBox.getItemCount(); i++) {
				// add selected items whose values are not empty strings
				// ie. ignore the "title" option at the top
				if (listBox.isItemSelected(i) && !listBox.getValue(i).equals("")) {
					selectedItemsList.add(listBox.getValue(i));
				}
			}
			if ( selectedItemsList.size() > 0 ) {
				searchParamsMap.put(key, selectedItemsList);
			}
		}
		return searchParamsMap;
	}

	@Override
	public void onResize() {
        int parentHeight = getParent().getOffsetHeight();
        int parentWidth = getParent().getOffsetWidth();
        int datasetsTableHeight = (int)parentHeight/3;
        int infoTableHeight = (int)parentHeight/3;
        datasetsScrollPanel.setHeight(datasetsTableHeight+"px");
        datasetsScrollPanel.setWidth(parentWidth-40+"px");
        infoScrollPanel.setHeight(infoTableHeight+"px");
        infoScrollPanel.setWidth(parentWidth-40+"px");
	}

}
