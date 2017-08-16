/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * The Apereo Foundation licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
*/
package org.unitime.timetable.gwt.client.solver;

import org.unitime.timetable.gwt.client.ToolBox;
import org.unitime.timetable.gwt.client.ToolBox.Page;
import org.unitime.timetable.gwt.client.page.UniTimeNotifications;
import org.unitime.timetable.gwt.client.widgets.LoadingWidget;
import org.unitime.timetable.gwt.client.widgets.P;
import org.unitime.timetable.gwt.client.widgets.SimpleForm;
import org.unitime.timetable.gwt.client.widgets.UniTimeHeaderPanel;
import org.unitime.timetable.gwt.command.client.GwtRpcService;
import org.unitime.timetable.gwt.command.client.GwtRpcServiceAsync;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.shared.CourseTimetablingSolverInterface.AssignedClassesFilterRequest;
import org.unitime.timetable.gwt.shared.CourseTimetablingSolverInterface.AssignedClassesFilterResponse;
import org.unitime.timetable.gwt.shared.CourseTimetablingSolverInterface.AssignedClassesRequest;
import org.unitime.timetable.gwt.shared.CourseTimetablingSolverInterface.AssignedClassesResponse;
import org.unitime.timetable.gwt.shared.EventInterface.EncodeQueryRpcRequest;
import org.unitime.timetable.gwt.shared.EventInterface.EncodeQueryRpcResponse;
import org.unitime.timetable.gwt.shared.FilterInterface;
import org.unitime.timetable.gwt.shared.SolverInterface.PageMessage;
import org.unitime.timetable.gwt.shared.SolverInterface.PageMessageType;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 * @author Tomas Muller
 */
public class AssignedClassesPage extends Composite {
	private static final GwtMessages MESSAGES = GWT.create(GwtMessages.class);
	protected static GwtRpcServiceAsync RPC = GWT.create(GwtRpcService.class);
	private PageFilter iFilter;
	private SimplePanel iRootPanel;
	private SimpleForm iPanel;
	private FilterInterface iLastFilter;
	private AssignedClassesResponse iLastResponse;
	private DataTable iTable;
	private PreferenceLegend iLegend;

	public AssignedClassesPage() {
		iFilter = new PageFilter();
		iFilter.getHeader().setCollapsible(SolverCookie.getInstance().isAssignedClassesFilter());
		iFilter.getHeader().addCollapsibleHandler(new ValueChangeHandler<Boolean>() {
			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				SolverCookie.getInstance().setAssignedClassesFilter(event.getValue());
			}
		});
		
		iPanel = new SimpleForm(2);
		iPanel.removeStyleName("unitime-NotPrintableBottomLine");
		iPanel.addRow(iFilter);
		
		iFilter.getFooter().addButton("search", MESSAGES.buttonSearch(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				search(null);
			}
		});
		iFilter.getFooter().setEnabled("search", false);
		iFilter.getFooter().addButton("print", MESSAGES.buttonPrint(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				print();
			}
		});
		iFilter.getFooter().setEnabled("print", false);
		iFilter.getFooter().addButton("exportCSV", MESSAGES.buttonExportCSV(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				exportData("csv");
			}
		});
		iFilter.getFooter().setEnabled("exportCSV", false);
		iFilter.getFooter().addButton("exportPDF", MESSAGES.buttonExportPDF(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				exportData("pdf");
			}
		});
		iFilter.getFooter().setEnabled("exportPDF", false);
		iRootPanel = new SimplePanel(iPanel);
		iRootPanel.addStyleName("unitime-AssignedClassesPage");
		initWidget(iRootPanel);
		init();
	}

	protected void init() {
		RPC.execute(new AssignedClassesFilterRequest(), new AsyncCallback<AssignedClassesFilterResponse>() {
			@Override
			public void onFailure(Throwable caught) {
				iFilter.getFooter().setErrorMessage(MESSAGES.failedToInitialize(caught.getMessage()));
				UniTimeNotifications.error(MESSAGES.failedToInitialize(caught.getMessage()), caught);
				ToolBox.checkAccess(caught);
			}

			@Override
			public void onSuccess(AssignedClassesFilterResponse result) {
				iLegend = new PreferenceLegend(result.getPreferences());
				iFilter.getFooter().clearMessage();
				iFilter.setValue(result);
				iFilter.getFooter().setEnabled("search", true);
				createTriggers();
				if (iFilter.getHeader().isCollapsible() != null && !iFilter.getHeader().isCollapsible())
					search(null);
			}
		});
	}
	
	public static native void createTriggers()/*-{
		$wnd.refreshPage = function() {
			@org.unitime.timetable.gwt.client.solver.AssignedClassesPage::__search()();
		};
	}-*/;
	
	public static void __search() {
		final int left = Window.getScrollLeft();
		final int top = Window.getScrollTop();
		AssignedClassesPage page = (AssignedClassesPage)RootPanel.get("UniTimeGWT:Body").getWidget(0);
		page.search(new AsyncCallback<Boolean>() {
			@Override
			public void onFailure(Throwable caught) {
			}
			@Override
			public void onSuccess(Boolean result) {
				if (result)
					Window.scrollTo(left, top);
			}
		});
	}
	
	protected void search(final AsyncCallback<Boolean> callback) {
		final AssignedClassesRequest request = new AssignedClassesRequest();
		request.setFilter(iFilter.getValue());
		iFilter.getFooter().clearMessage();
		for (int row = iPanel.getRowCount() - 1; row > 0; row--)
			iPanel.removeRow(row);
		iFilter.getFooter().showLoading();
		iFilter.getFooter().setEnabled("search", false);
		LoadingWidget.showLoading(MESSAGES.waitLoadingData());
		RPC.execute(request, new AsyncCallback<AssignedClassesResponse>() {
			@Override
			public void onFailure(Throwable caught) {
				LoadingWidget.hideLoading();
				iFilter.getFooter().setErrorMessage(MESSAGES.failedToLoadAssignedClasses(caught.getMessage()));
				UniTimeNotifications.error(MESSAGES.failedToLoadAssignedClasses(caught.getMessage()), caught);
				iFilter.getFooter().setEnabled("search", true);
				if (callback != null)
					callback.onFailure(caught);
			}

			@Override
			public void onSuccess(AssignedClassesResponse result) {
				LoadingWidget.hideLoading();
				iFilter.getFooter().clearMessage();
				populate(request.getFilter(), result);
				iFilter.getFooter().setEnabled("search", true);
				if (callback != null)
					callback.onSuccess(!result.getRows().isEmpty());
			}
		});
	}
	
	protected void print() {
		final DataTable table = new DataTable(iLastResponse);
		Element headerRow = table.getRowFormatter().getElement(0);
		Element tableElement = table.getElement();
		Element thead = DOM.createTHead();
		tableElement.insertFirst(thead);
		headerRow.getParentElement().removeChild(headerRow);
		thead.appendChild(headerRow);
		Page page = new Page() {
			@Override
			public String getName() {
				return MESSAGES.sectAssignedClasses();
			}
			@Override
			public String getUser() {
				return "";
			}
			@Override
			public String getSession() {
				return "";
			}
			@Override
			public Element getBody() {
				return table.getElement();
			}
		};
		ToolBox.print(page);
	}
	
	private void exportData(String format) {
		String query = "output=assigned-classes." + format + iFilter.getQuery() + "&sort=" + SolverCookie.getInstance().getAssignedClassesSort();
		RPC.execute(EncodeQueryRpcRequest.encode(query), new AsyncCallback<EncodeQueryRpcResponse>() {
			@Override
			public void onFailure(Throwable caught) {
			}
			@Override
			public void onSuccess(EncodeQueryRpcResponse result) {
				ToolBox.open(GWT.getHostPageBaseURL() + "export?q=" + result.getQuery());
			}
		});
	}
	
	protected void populate(FilterInterface filter, AssignedClassesResponse response) {
		iLastFilter = filter;
		iLastResponse = response;
		iFilter.getFooter().setEnabled("print", false);
		iFilter.getFooter().setEnabled("exportCSV", false);
		iFilter.getFooter().setEnabled("exportPDF", false);
		for (int row = iPanel.getRowCount() - 1; row > 0; row--)
			iPanel.removeRow(row);
		
		RootPanel cpm = RootPanel.get("UniTimeGWT:CustomPageMessages");
		if (cpm != null) {
			cpm.clear();
			if (response.hasPageMessages()) {
				for (final PageMessage pm: response.getPageMessages()) {
					P p = new P(pm.getType() == PageMessageType.ERROR ? "unitime-PageError" : pm.getType() == PageMessageType.WARNING ? "unitime-PageWarn" : "unitime-PageMessage");
					p.setHTML(pm.getMessage());
					if (pm.hasUrl()) {
						p.addStyleName("unitime-ClickablePageMessage");
						p.addClickHandler(new ClickHandler() {
							@Override
							public void onClick(ClickEvent event) {
								if (pm.hasUrl()) ToolBox.open(GWT.getHostPageBaseURL() + pm.getUrl());
							}
						});
					}
					cpm.add(p);
				}
			}
		}
		
		if (response.getRows().isEmpty()) {
			iFilter.getFooter().setMessage(MESSAGES.errorAssignedClassesNoDataReturned());
			return;
		}
		
		UniTimeHeaderPanel header = new UniTimeHeaderPanel(MESSAGES.sectAssignedClasses());
		iPanel.addHeaderRow(header);
		if (iTable == null) {
			iTable = new DataTable(response);
			iTable.addValueChangeHandler(new ValueChangeHandler<Integer>() {
				@Override
				public void onValueChange(ValueChangeEvent<Integer> event) {
					SolverCookie.getInstance().setAssignedClassesSort(event.getValue() == null ? 0 : event.getValue().intValue());
				}
			});
		} else {
			iTable.populate(response);
		}
		iTable.setValue(SolverCookie.getInstance().getAssignedClassesSort());
		iPanel.addRow(iTable);
		iPanel.addRow(iLegend);
		
		iFilter.getFooter().setEnabled("print", true);
		iFilter.getFooter().setEnabled("exportCSV", true);
		iFilter.getFooter().setEnabled("exportPDF", true);
	}
}
