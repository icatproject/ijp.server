package org.icatproject.ijp.client;

import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;

/**
 * Composite widget to define selection lists/tables for Datasets and Datafiles.
 * @author Brian Ritchie
 *
 */
public class SelectionListPanel extends Composite {

	private static SelectionListPanelUiBinder uiBinder = GWT
			.create(SelectionListPanelUiBinder.class);

	interface SelectionListPanelUiBinder extends
			UiBinder<Widget, SelectionListPanel> {
	}

	public SelectionListPanel() {
		this("Current Selection");
	}

	@UiField
	Label titleLabel;
	
	@UiField
	ScrollPanel selectionTableHolder;

	CellTable<SelectionListContent> selectionTable;
	
	// "Remove Selected" items from the list
	@UiField
	Button removeSelectedButton;
	
	// "Remove All" items from the list
	@UiField
	Button removeAllButton;
	
	// "Do something" with the current selection set
	// Caller will want to set the click handler for this button
	@UiField 
	Button acceptButton;
	
	private boolean acceptButtonHasHandler = false;
	
	// "Cancel" (e.g. close containing dialog and forget everything)
	// Caller may not want to use this button.
	@UiField
	Button cancelButton;
	
	private boolean cancelButtonHasHandler = false;
	
	private ListDataProvider<SelectionListContent> selectionListModel = new ListDataProvider<SelectionListContent>();
	
	// Perhaps this should be called selectionSelectionModel by analogy with selectionListModel :-)
	final MultiSelectionModel<SelectionListContent> selectionModel = new MultiSelectionModel<SelectionListContent>();

	public SelectionListPanel(String title) {
		
		initWidget(uiBinder.createAndBindUi(this));
		selectionTable = new CellTable<SelectionListContent>();
		selectionTable.setWidth("100%");
		
		selectionListModel.addDataDisplay(selectionTable);
		
		selectionTable.setSelectionModel(selectionModel);
		selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
			public void onSelectionChange(SelectionChangeEvent event) {
				Set<SelectionListContent> selectedContents = selectionModel.getSelectedSet();
				if( selectedContents.size() == 0 ){
					removeSelectedButton.setEnabled(false);
					// Note: the Accept button operates on the whole cart,
					// so its state is independent of the selection
				} else {
					removeSelectedButton.setEnabled(true);
					removeAllButton.setEnabled(true);
				}
			}
		});
		
		selectionTable.setRowCount(0);
		
		selectionTableHolder.add(selectionTable);

		removeSelectedButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				// Remove each selected element from the table
				// by removing them from the list then repopulating the table
				boolean atLeastOneRemoved = false;
				for( SelectionListContent item : selectionModel.getSelectedSet() ){
					selectionListModel.getList().remove(item);
					atLeastOneRemoved = true;
				}
				if( atLeastOneRemoved ){
					selectionListModel.refresh();
					selectionModel.clear();
					removeSelectedButton.setEnabled(false);
					checkAcceptButtonEnabled();
					// If everything has been removed, disabled the remove buttons
					// Do we want to hide the Panel completely?
					if( selectionListModel.getList().size() == 0 ){
						removeAllButton.setEnabled(false);
					}
				}
			}
		});

		removeAllButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				clear();
				removeAllButton.setEnabled(false);
				removeSelectedButton.setEnabled(false);
				// Note: will it ever make sense to Accept an empty cart? This assumes that it won't.
				acceptButton.setEnabled(false);
			}
		});
		
		acceptButton.setText("Use Selected");
		
		// Accept button should be enabled iff we have some content; initially we don't
		acceptButton.setEnabled(false);
		
		// Similarly, the removeSelected and removeAll buttons should be disabled initially
		removeSelectedButton.setEnabled(false);
		removeAllButton.setEnabled(false);
		
		// Hide and disable the cancel button - caller must use addCancelHandler() to see/enable it
		cancelButton.setText("Cancel");
		cancelButton.setEnabled(false);
		cancelButton.setVisible(false);
	}

	public void setTitle(String text) {
		titleLabel.setText(text);
	}

	public String getTitle() {
		return titleLabel.getText();
	}
	
	public void setColumnsFrom( SelectionListContent sampleContent ){
		
		for( final String columnName : sampleContent.availableColumns() ){
			TextColumn<SelectionListContent> column = new TextColumn<SelectionListContent>() {
				@Override
				public String getValue(SelectionListContent content) {
					return content.getColumn(columnName);
				}
			};
			selectionTable.addColumn(column, columnName);
		}
	}
	
	private void checkAcceptButtonEnabled(){
		if( selectionListModel.getList().size() > 0 ){
			acceptButton.setEnabled(true);
		} else {
			acceptButton.setEnabled(false);
		}
	}
	
	/**
	 * Add the supplied contentItem to the selection table, if it is not already in there.
	 * 
	 * @param contentItem
	 */
	public void addItem(SelectionListContent contentItem){
		if( ! selectionListModel.getList().contains(contentItem) ){
			selectionListModel.getList().add(contentItem);
			selectionListModel.refresh();
			acceptButton.setEnabled(true);
			removeAllButton.setEnabled(true);
		}
	}
	
	/**
	 * Add the supplied list to the selection list.
	 * Duplicates will be ignored.
	 * 
	 * @param contents
	 */
	public void addContent( List<SelectionListContent> contents ){
		boolean atLeastOneAdded = false;
		for( SelectionListContent item : contents ){
			if( ! selectionListModel.getList().contains(item) ){
				selectionListModel.getList().add(item);
				atLeastOneAdded = true;
			}
		}
		if( atLeastOneAdded ){
			selectionListModel.refresh();
			checkAcceptButtonEnabled();
			removeAllButton.setEnabled(true);
		}
	}
	
	/**
	 * Use the supplied list of contents as the selection list contents.
	 * This will replace any existing contents.
	 * Duplicates in the contents will only be added once.
	 * 
	 * @param contents
	 */
	public void setContent( List<SelectionListContent> contents ){
		selectionListModel.getList().clear();
		this.addContent(contents);
		if( contents.size() > 0 ){
			removeAllButton.setEnabled(true);
		}
		selectionModel.clear();
		checkAcceptButtonEnabled();
	}
	
	public void addAcceptHandler( ClickHandler clickHandler ){
		acceptButtonHasHandler = true;
		acceptButton.addClickHandler(clickHandler);
	}
	
	public void addCancelHandler( ClickHandler clickHandler ){
		cancelButtonHasHandler = true;
		cancelButton.addClickHandler(clickHandler);
		cancelButton.setEnabled(true);
		cancelButton.setVisible(true);
	}
	
	public void setAcceptButtonText(String title){
		acceptButton.setText(title);
	}
	
	public Set<SelectionListContent> getSelection(){
		return selectionModel.getSelectedSet();
	}
	
	public List<SelectionListContent> getEverything() {
		return selectionListModel.getList();
	}

	public void setVisible(boolean b){
		titleLabel.setVisible(b);
		selectionTableHolder.setVisible(b);
		removeSelectedButton.setVisible(b);
		removeAllButton.setVisible(b);
		acceptButton.setVisible(b);
		// Only show the cancel button if it has a handler defined
		cancelButton.setVisible(cancelButtonHasHandler && b);
	}
	
	public boolean isVisible(){
		return titleLabel.isVisible();
	}

	public void clear() {
		selectionListModel.getList().clear();
		selectionListModel.refresh();
		selectionModel.clear();
		acceptButton.setEnabled(false);
	}

}