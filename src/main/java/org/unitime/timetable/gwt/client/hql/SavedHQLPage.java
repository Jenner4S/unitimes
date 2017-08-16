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
package org.unitime.timetable.gwt.client.hql;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.unitime.timetable.gwt.client.ToolBox;
import org.unitime.timetable.gwt.client.page.UniTimeNotifications;
import org.unitime.timetable.gwt.client.page.UniTimePageLabel;
import org.unitime.timetable.gwt.client.widgets.LoadingWidget;
import org.unitime.timetable.gwt.client.widgets.SimpleForm;
import org.unitime.timetable.gwt.client.widgets.UniTimeDialogBox;
import org.unitime.timetable.gwt.client.widgets.UniTimeHeaderPanel;
import org.unitime.timetable.gwt.client.widgets.UniTimeTable;
import org.unitime.timetable.gwt.client.widgets.UniTimeTableHeader;
import org.unitime.timetable.gwt.client.widgets.UniTimeTextBox;
import org.unitime.timetable.gwt.client.widgets.UniTimeWidget;
import org.unitime.timetable.gwt.command.client.GwtRpcResponseBoolean;
import org.unitime.timetable.gwt.command.client.GwtRpcResponseList;
import org.unitime.timetable.gwt.command.client.GwtRpcResponseLong;
import org.unitime.timetable.gwt.command.client.GwtRpcResponseNull;
import org.unitime.timetable.gwt.command.client.GwtRpcService;
import org.unitime.timetable.gwt.command.client.GwtRpcServiceAsync;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.shared.SavedHQLInterface;
import org.unitime.timetable.gwt.shared.EventInterface.EncodeQueryRpcRequest;
import org.unitime.timetable.gwt.shared.EventInterface.EncodeQueryRpcResponse;
import org.unitime.timetable.gwt.shared.SavedHQLInterface.HQLDeleteRpcRequest;
import org.unitime.timetable.gwt.shared.SavedHQLInterface.HQLExecuteRpcRequest;
import org.unitime.timetable.gwt.shared.SavedHQLInterface.HQLOptionsInterface;
import org.unitime.timetable.gwt.shared.SavedHQLInterface.HQLOptionsRpcRequest;
import org.unitime.timetable.gwt.shared.SavedHQLInterface.HQLQueriesRpcRequest;
import org.unitime.timetable.gwt.shared.SavedHQLInterface.HQLSetBackRpcRequest;
import org.unitime.timetable.gwt.shared.SavedHQLInterface.HQLStoreRpcRequest;
import org.unitime.timetable.gwt.shared.SavedHQLInterface.Query;
import org.unitime.timetable.gwt.shared.SavedHQLInterface.Table;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Tomas Muller
 */
public class SavedHQLPage extends Composite {
	protected static final GwtMessages MESSAGES = GWT.create(GwtMessages.class);
	private static final GwtRpcServiceAsync RPC = GWT.create(GwtRpcService.class);
	
	private SimpleForm iForm = null;
	private UniTimeHeaderPanel iHeader = null, iTableHeader = null, iTableFooter = null;
	private UniTimeWidget<ListBox> iQuerySelector = null;
	private HTML iDescription = null;
	
	private List<SavedHQLInterface.Query> iQueries = new ArrayList<SavedHQLInterface.Query>();
	private List<SavedHQLInterface.Flag> iFlags = new ArrayList<SavedHQLInterface.Flag>();
	private List<SavedHQLInterface.Option> iOptions = new ArrayList<SavedHQLInterface.Option>();
	private UniTimeTable<String[]> iTable = new UniTimeTable<String[]>();
	private String iFirstField = null;
	private String iAppearance = null;
	private int iFirstLine = 0;
	private int iLastSort = 0;
	private String iLastHistory = null;
	
	public SavedHQLPage() {
		iAppearance = Window.Location.getParameter("appearance");
		if ("courses".equalsIgnoreCase(iAppearance)) {
			UniTimePageLabel.getInstance().setPageName(MESSAGES.pageCourseReports());
		} else if ("exams".equalsIgnoreCase(iAppearance)) {
				UniTimePageLabel.getInstance().setPageName(MESSAGES.pageExaminationReports());
		} else if ("sectioning".equalsIgnoreCase(iAppearance)) {
			UniTimePageLabel.getInstance().setPageName(MESSAGES.pageStudentSectioningReports());
		} else if ("events".equalsIgnoreCase(iAppearance)) {
			UniTimePageLabel.getInstance().setPageName(MESSAGES.pageEventReports());
		} else if ("administration".equalsIgnoreCase(iAppearance)) {
			UniTimePageLabel.getInstance().setPageName(MESSAGES.pageAdministrationReports());
		} else {
			iAppearance = "courses";
			UniTimePageLabel.getInstance().setPageName(MESSAGES.pageCourseReports());
		}
		
		iForm = new SimpleForm(2);
		
		iForm.removeStyleName("unitime-NotPrintableBottomLine");
		
		iHeader = new UniTimeHeaderPanel(MESSAGES.sectFilter());
		iHeader.addButton("execute", MESSAGES.buttonExecute(), 75, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				iFirstLine = 0;
				iLastSort = 0;
				execute();
			}
		});
		
		iHeader.addButton("print", MESSAGES.buttonPrint(), 75, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				HQLExecuteRpcRequest request = new HQLExecuteRpcRequest();
				Long id = Long.valueOf(iQuerySelector.getWidget().getValue(iQuerySelector.getWidget().getSelectedIndex()));
				for (SavedHQLInterface.Query q: iQueries) {
					if (id.equals(q.getId())) {
						request.setQuery(q); break;
					}
				}
				if (request.getQuery() == null) {
					iHeader.setErrorMessage(MESSAGES.errorNoReportSelected());
					return;
				}
				final SimpleForm form = new SimpleForm();
				form.addHeaderRow(request.getQuery().getName());
				if (!request.getQuery().getDescription().isEmpty())
					form.addRow(MESSAGES.propDescription(), new HTML(request.getQuery().getDescription()));
				
				for (int i = 0; i < iOptions.size(); i++) {
					SavedHQLInterface.Option option = iOptions.get(i);
					if (request.getQuery().getQuery().contains("%" + option.getType() + "%")) {
						ListBox list = ((UniTimeWidget<ListBox>)iForm.getWidget(3 + i, 1)).getWidget();
						String value = "";
						String values = "";
						boolean allSelected = true;
						if (list.isMultipleSelect()) {
							for (int j = 0; j < list.getItemCount(); j++)
								if (list.isItemSelected(j)) {
									if (!value.isEmpty()) { value += ","; values += ", "; }
									value += list.getValue(j);
									values += list.getItemText(j);
								} else {
									allSelected = false;
								}
							if (allSelected) values = MESSAGES.itemAll();
						} else if (list.getSelectedIndex() > 0) {
							value = list.getValue(list.getSelectedIndex());
							values = list.getItemText(list.getSelectedIndex());
						}
						if (value.isEmpty()) {
							iHeader.setErrorMessage(MESSAGES.errorItemNotSelected(option.getName()));
							return;
						}
						request.addOption(option.getType(), value);
						form.addRow(option.getName() + ":", new Label(values, true));
					}
				}
				LoadingWidget.getInstance().show(MESSAGES.waitExecuting(request.getQuery().getName()));
				request.setFromRow(0); request.setMaxRows(10000);
				RPC.execute(request, new AsyncCallback<Table>() {
					@Override
					public void onFailure(Throwable caught) {
						iTableHeader.setErrorMessage(caught.getMessage());
						LoadingWidget.getInstance().hide();
					}

					@Override
					public void onSuccess(Table result) {
						LoadingWidget.getInstance().hide();
						if (result == null || result.size() <= 1) {
							iTableHeader.setMessage(MESSAGES.errorNoResults());
							return;
						} 
						final UniTimeTable<String[]> table = new UniTimeTable<String[]>();
						String firstField = null;
						int nrCols = 0;
						for (int i = 0; i < result.size(); i++) {
							String[] row = result.get(i);
							List<Widget> line = new ArrayList<Widget>();
							if (i == 0) {
								firstField = row[0]; nrCols = row.length;
								for (String x: row) {
									final String name = x.replace('_', ' ').trim();
									UniTimeTableHeader h = new UniTimeTableHeader(name, 1);
									line.add(h);
								}
							} else {
								for (String x: row) {
									line.add(new HTML(x == null ? "" : x.replace("\\n", "<br>")));
								}
							}
							table.addRow(i == 0 ? null : row, line);
						}
						if (firstField != null && firstField.startsWith("__"))
							table.setColumnVisible(0, false);
						if (iLastSort != 0 && Math.abs(iLastSort) <= nrCols) {
							table.sort(table.getHeader(Math.abs(iLastSort) - 1), new Comparator<String[]>() {
								@Override
								public int compare(String[] o1, String[] o2) {
									return SavedHQLPage.compare(o1, o2, Math.abs(iLastSort) - 1);
								}
							}, iLastSort > 0);
						}
						table.getElement().getStyle().setWidth(1040, Unit.PX);
						
						// Move header row to thead
						Element headerRow = table.getRowFormatter().getElement(0);
						Element tableElement = table.getElement();
						Element thead = DOM.createTHead();
						tableElement.insertFirst(thead);
						headerRow.getParentElement().removeChild(headerRow);
						thead.appendChild(headerRow);
						
						final Element div = DOM.createDiv();
						div.appendChild(form.getElement());
						div.appendChild(table.getElement());
						
						String name = MESSAGES.pageCourseReports();
						if ("courses".equalsIgnoreCase(iAppearance)) {
							name = MESSAGES.pageCourseReports();
						} else if ("exams".equalsIgnoreCase(iAppearance)) {
							name = MESSAGES.pageExaminationReports();
						} else if ("sectioning".equalsIgnoreCase(iAppearance)) {
							name = MESSAGES.pageStudentSectioningReports();
						} else if ("events".equalsIgnoreCase(iAppearance)) {
							name = MESSAGES.pageEventReports();
						} else if ("administration".equalsIgnoreCase(iAppearance)) {
							name = MESSAGES.pageAdministrationReports();
						}
						final String pageName = name;

						ToolBox.print(new ToolBox.Page() {
							@Override
							public String getName() { return pageName; }
							@Override
							public String getUser() { return ""; }
							@Override
							public String getSession() { return ""; }
							@Override
							public Element getBody() { return div; }
						});
						
					}
				});	
			}
		});

		iHeader.addButton("export", MESSAGES.buttonExportCSV(), 85, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				Long id = Long.valueOf(iQuerySelector.getWidget().getValue(iQuerySelector.getWidget().getSelectedIndex()));
				SavedHQLInterface.Query query = null;
				for (SavedHQLInterface.Query q: iQueries) {
					if (id.equals(q.getId())) {
						query = q; break;
					}
				}
				if (query == null) {
					iHeader.setErrorMessage(MESSAGES.errorNoReportSelected());
					return;
				}
				String params = "";
				for (int i = 0; i < iOptions.size(); i++) {
					SavedHQLInterface.Option option = iOptions.get(i);
					if (query.getQuery().contains("%" + option.getType() + "%")) {
						ListBox list = ((UniTimeWidget<ListBox>)iForm.getWidget(3 + i, 1)).getWidget();
						String value = "";
						boolean allSelected = true;
						if (list.isMultipleSelect()) {
							for (int j = 0; j < list.getItemCount(); j++)
								if (list.isItemSelected(j)) {
									if (!value.isEmpty()) value += ",";
									value += list.getValue(j);
								} else {
									allSelected = false;
								}
						} else if (list.getSelectedIndex() > 0) {
							value = list.getValue(list.getSelectedIndex());
						}
						if (value.isEmpty()) {
							iHeader.setErrorMessage(MESSAGES.errorItemNotSelected(option.getName()));
							return;
						}
						if (!params.isEmpty()) params += ":";
						params += (list.isMultipleSelect() && allSelected ? "" : value);
					}
				}
				String reportId = iQuerySelector.getWidget().getValue(iQuerySelector.getWidget().getSelectedIndex());
				
				RPC.execute(EncodeQueryRpcRequest.encode("output=hql-report.csv&report=" + reportId + "&params=" + params + "&sort=" + iLastSort), new AsyncCallback<EncodeQueryRpcResponse>() {
					@Override
					public void onFailure(Throwable caught) {
					}
					@Override
					public void onSuccess(EncodeQueryRpcResponse result) {
						ToolBox.open(GWT.getHostPageBaseURL() + "export?q=" + result.getQuery());
					}
				});
			}
		});

		
		iHeader.addButton("edit", MESSAGES.buttonEdit(), 75, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				SavedHQLInterface.Query query = null;
				Long id = Long.valueOf(iQuerySelector.getWidget().getValue(iQuerySelector.getWidget().getSelectedIndex()));
				for (SavedHQLInterface.Query q: iQueries) {
					if (id.equals(q.getId())) {
						query = q; break;
					}
				}
				if (query == null) {
					iHeader.setErrorMessage(MESSAGES.errorNoReportSelected());
					return;
				}
				openDialog(query);
			}
		});
		
		iHeader.addButton("add", MESSAGES.buttonAddNew(), 75, new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				openDialog(null);
			}
		});

		iForm.addHeaderRow(iHeader);
		iHeader.setEnabled("execute", false);
		iHeader.setEnabled("edit", false);
		iHeader.setEnabled("add", false);
		iHeader.setEnabled("print", false);
		iHeader.setEnabled("export", false);

		iForm.getColumnFormatter().setWidth(0, "120px");
		iForm.getColumnFormatter().setWidth(1, "100%");

		iQuerySelector = new UniTimeWidget<ListBox>(new ListBox());
		iForm.addRow(MESSAGES.propReport(), iQuerySelector);
		iQuerySelector.getWidget().addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				iQuerySelector.clearHint();
				iQuerySelector.setPrintText(iQuerySelector.getWidget().getItemText(iQuerySelector.getWidget().getSelectedIndex()));
				queryChanged();
			}
		});
		iDescription = new HTML("");
		iForm.addRow(MESSAGES.propDescription(), iDescription);
		iForm.getCellFormatter().setVerticalAlignment(2, 0, HasVerticalAlignment.ALIGN_TOP);
		
		LoadingWidget.getInstance().show(MESSAGES.waitLoadingReports());
		RPC.execute(new HQLOptionsRpcRequest(), new AsyncCallback<HQLOptionsInterface>() {
			@Override
			public void onFailure(Throwable caught) {
				iHeader.setErrorMessage(caught.getMessage());
				LoadingWidget.getInstance().hide();
				ToolBox.checkAccess(caught);
			}

			@Override
			public void onSuccess(HQLOptionsInterface result) {
				iFlags = result.getFlags();
				iOptions = result.getOptions();
				iHeader.setEnabled("add", result.isEditable());
				for (int i = 0; i < iOptions.size(); i++) {
					SavedHQLInterface.Option option = iOptions.get(i);
					ListBox list = new ListBox();
					list.setMultipleSelect(option.isMultiSelect());
					if (!option.isMultiSelect())
						list.addItem(MESSAGES.itemSelect(), "-1");
					for (SavedHQLInterface.IdValue v: option.values())
						list.addItem(v.getText(), v.getValue());
					final UniTimeWidget<ListBox> u = new UniTimeWidget<ListBox>(list);
					iForm.addRow(option.getName() + ":", u);
					iForm.getCellFormatter().setVerticalAlignment(3 + i, 0, HasVerticalAlignment.ALIGN_TOP);
					iForm.getRowFormatter().setVisible(3 + i, false);
					if (list.isMultipleSelect()) {
						for (int j = 0; j < list.getItemCount(); j++)
							list.setItemSelected(j, true);
						u.setPrintText(MESSAGES.itemAll());
					} else if (list.getItemCount() == 2) {
						list.setSelectedIndex(1);
						u.setPrintText(list.getItemText(1));
					}
					list.addChangeHandler(new ChangeHandler() {
						@Override
						public void onChange(ChangeEvent event) {
							u.clearHint();
							String selected = "";
							boolean hasAll = true;
							for (int i = 0; i < u.getWidget().getItemCount(); i++) {
								if (u.getWidget().isItemSelected(i)) {
									if (!selected.isEmpty()) selected += ",";
									selected += u.getWidget().getItemText(i);
								} else hasAll = false;
							}
							if (hasAll && u.getWidget().getItemCount() > 5)
								selected = MESSAGES.itemAll();
							if (selected.length() > 150)
								selected = selected.substring(0, 147) + "...";
							u.setPrintText(selected);
							iHeader.clearMessage();
						}
					});
				}
				iTableHeader = new UniTimeHeaderPanel(MESSAGES.sectResults());
				iTableHeader.addButton("previous", MESSAGES.buttonPrevious(), 75, new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						iFirstLine -= 100;
						execute();
					}
				});
				iTableHeader.addButton("next", MESSAGES.buttonNext(), 75, new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						iFirstLine += 100;
						execute();
					}
				});
				iTableHeader.setEnabled("previous", false);
				iTableHeader.setEnabled("next", false);
				iForm.addHeaderRow(iTableHeader);
				iForm.addRow(iTable);
				iTableFooter = iTableHeader.clonePanel("");
				iTableFooter.setVisible(false);
				iForm.addRow(iTableFooter);
				iForm.addBottomRow(iHeader.clonePanel(""));
				loadQueries(null, true);
			}
		});

		iTable.addMouseClickListener(new UniTimeTable.MouseClickListener<String[]>() {
			@Override
			public void onMouseClick(UniTimeTable.TableEvent<String[]> event) {
				if (event.getRow() > 0 && event.getData() != null) {
					if ("__Class".equals(iFirstField))
						ToolBox.open(GWT.getHostPageBaseURL() + "classDetail.do?cid=" + event.getData()[0]);
					else if ("__Offering".equals(iFirstField))
						ToolBox.open(GWT.getHostPageBaseURL() + "instructionalOfferingDetail.do?op=view&io=" + event.getData()[0]);
					else if ("__Subpart".equals(iFirstField))
						ToolBox.open(GWT.getHostPageBaseURL() + "schedulingSubpartDetail.do?ssuid=" + event.getData()[0]);
					else if ("__Room".equals(iFirstField))
						ToolBox.open(GWT.getHostPageBaseURL() + "gwt.jsp?page=rooms&back=1&id=" + event.getData()[0]);
					else if ("__Instructor".equals(iFirstField))
						ToolBox.open(GWT.getHostPageBaseURL() + "instructorDetail.do?instructorId=" + event.getData()[0]);
					else if ("__Exam".equals(iFirstField))
						ToolBox.open(GWT.getHostPageBaseURL() + "examDetail.do?examId=" + event.getData()[0]);
					else if ("__Event".equals(iFirstField))
						ToolBox.open(GWT.getHostPageBaseURL() + "gwt.jsp?page=events#event=" + event.getData()[0]);
				}
			}
		});
		
		initWidget(iForm);
		
		History.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				reload(event.getValue());
			}
		});
	}
	
	UniTimeDialogBox iDialog = null;
	SimpleForm iDialogForm = null;
	UniTimeHeaderPanel iDialogHeader = null;
	SavedHQLInterface.Query iDialogQuery = null;
	UniTimeTextBox iDialogName = null;
	TextArea iDialogDescription = null;
	TextArea iDialogQueryArea = null;
	ListBox iDialogAppearance = null;
	
	public void openDialog(SavedHQLInterface.Query q) {
		iDialogQuery = q;
		if (iDialog == null) {
			iDialog = new UniTimeDialogBox(true, false);
			iDialogForm = new SimpleForm();
			iDialogName = new UniTimeTextBox(100, 680);
			iDialogForm.addRow(MESSAGES.propName(), iDialogName);
			iDialogDescription = new TextArea();
			iDialogDescription.setStyleName("unitime-TextArea");
			iDialogDescription.setVisibleLines(5);
			iDialogDescription.setCharacterWidth(120);
			iDialogForm.addRow(MESSAGES.propDescription(), iDialogDescription);
			iDialogForm.getCellFormatter().setVerticalAlignment(1, 0, HasVerticalAlignment.ALIGN_TOP);
			iDialogQueryArea = new TextArea();
			iDialogQueryArea.setStyleName("unitime-TextArea");
			iDialogQueryArea.setVisibleLines(10);
			iDialogQueryArea.setCharacterWidth(120);
			iDialogForm.addRow(MESSAGES.propQuery(), iDialogQueryArea);
			iDialogForm.getCellFormatter().setVerticalAlignment(2, 0, HasVerticalAlignment.ALIGN_TOP);
			for (int i = 0; i < iFlags.size(); i++) {
				SavedHQLInterface.Flag f = iFlags.get(i);
				CheckBox ch = new CheckBox(f.getText());
				iDialogForm.addRow(i == 0 ? MESSAGES.propFlags() : "", ch);
			}
			iDialogHeader = new UniTimeHeaderPanel();
			iDialogForm.addBottomRow(iDialogHeader);
			iDialogHeader.addButton("save", MESSAGES.opQuerySave(), 75, new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					iDialogQuery.setName(iDialogName.getText());
					if (iDialogName.getText().isEmpty()) {
						iDialogHeader.setErrorMessage(MESSAGES.errorNameIsRequired());
						return;
					}
					iDialogQuery.setDescription(iDialogDescription.getText());
					iDialogQuery.setQuery(iDialogQueryArea.getText());
					if (iDialogQueryArea.getText().isEmpty()) {
						iDialogHeader.setErrorMessage(MESSAGES.errorQueryIsRequired());
						return;
					}
					int flags = 0;
					boolean hasAppearance = false;
					for (int i = 0; i < iFlags.size(); i++) {
						SavedHQLInterface.Flag f = iFlags.get(i);
						CheckBox ch = (CheckBox)iDialogForm.getWidget(3 + i, 1);
						if (ch.getValue()) {
							flags += f.getValue();
							if (f.isAppearance()) hasAppearance = true;
						}
					}
					if (!hasAppearance) {
						iDialogHeader.setErrorMessage(MESSAGES.errorNoAppearanceSelected());
						return;
					}
					iDialogQuery.setFlags(flags);
					RPC.execute(new HQLStoreRpcRequest(iDialogQuery), new AsyncCallback<GwtRpcResponseLong>() {
						@Override
						public void onFailure(Throwable caught) {
							iDialogHeader.setErrorMessage(caught.getMessage());
						}

						@Override
						public void onSuccess(GwtRpcResponseLong result) {
							iDialog.hide();
							loadQueries(result.getValue(), false);
						}
					});
				}
			});
			iDialogHeader.addButton("test", MESSAGES.opQueryTest(), 75, new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					if (iDialogQueryArea.getText().isEmpty()) {
						iDialogHeader.setErrorMessage(MESSAGES.errorQueryIsRequired());
						return;
					}
					iDialogQuery.setQuery(iDialogQueryArea.getText());
					LoadingWidget.getInstance().show(MESSAGES.waitTestingQuery());
					HQLExecuteRpcRequest request = new HQLExecuteRpcRequest();
					request.setQuery(iDialogQuery);
					request.setFromRow(0);
					request.setMaxRows(101);
					RPC.execute(request, new AsyncCallback<Table>() {
						@Override
						public void onFailure(Throwable caught) {
							iDialogHeader.setErrorMessage(MESSAGES.failedTestNoReason());
							LoadingWidget.getInstance().hide();
							UniTimeNotifications.error(MESSAGES.failedTest(caught.getMessage()), caught);
						}

						@Override
						public void onSuccess(Table result) {
							iDialogHeader.setMessage(result.size() <= 1 ? MESSAGES.infoTestSucceededNoResults() : result.size() > 101 ? MESSAGES.infoTestSucceededWith100OrMoreRows() : MESSAGES.infoTestSucceededWithRows(result.size() - 1));
							LoadingWidget.getInstance().hide();
						}
					});
				}
			});
			iDialogHeader.addButton("export", MESSAGES.opScriptExport(), new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					RPC.execute(EncodeQueryRpcRequest.encode("output=hql.xml&id=" + iDialogQuery.getId()), new AsyncCallback<EncodeQueryRpcResponse>() {
						@Override
						public void onFailure(Throwable caught) {
						}
						@Override
						public void onSuccess(EncodeQueryRpcResponse result) {
							ToolBox.open(GWT.getHostPageBaseURL() + "export?q=" + result.getQuery());
						}
					});
				}
			});
			iDialogHeader.addButton("delete", MESSAGES.opQueryDelete(), 75, new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					RPC.execute(new HQLDeleteRpcRequest(iDialogQuery.getId()), new AsyncCallback<GwtRpcResponseBoolean>() {
						@Override
						public void onFailure(Throwable caught) {
							iDialogHeader.setErrorMessage(caught.getMessage());
						}

						@Override
						public void onSuccess(GwtRpcResponseBoolean result) {
							iDialog.hide();
							loadQueries(null, false);
						}
					});
				}
			});
			iDialogHeader.addButton("back", MESSAGES.opQueryBack(), 75, new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					iDialog.hide();
				}
			});
			iDialog.setWidget(iDialogForm);
		}
		iDialog.setText(q == null ? MESSAGES.dialogNewReport() : MESSAGES.dialogEditReport(q.getName()));
		iDialogHeader.setEnabled("export", q != null);
		iDialogHeader.setEnabled("delete", q != null);
		iDialogHeader.clearMessage();
		iDialogName.setText(q == null ? "" : q.getName());
		iDialogDescription.setText(q == null ? "" : q.getDescription());
		iDialogQueryArea.setText(q == null ? "" : q.getQuery());
		for (int i = 0; i < iFlags.size(); i++) {
			SavedHQLInterface.Flag f = iFlags.get(i);
			CheckBox ch = (CheckBox)iDialogForm.getWidget(3 + i, 1);
			ch.setValue(q == null ? iAppearance.equals(f.getAppearance()) : (q.getFlags() & f.getValue()) != 0); 
		}
		if (iDialogQuery == null) iDialogQuery = new SavedHQLInterface.Query();
		iDialog.center();
	}
	
	public void loadQueries(final Long select, final boolean reload) {
		if (!LoadingWidget.getInstance().isShowing())
			LoadingWidget.getInstance().show(MESSAGES.waitLoadingReports());
		RPC.execute(new HQLQueriesRpcRequest(iAppearance), new AsyncCallback<GwtRpcResponseList<Query>>() {
			@Override
			public void onFailure(Throwable caught) {
				iHeader.setErrorMessage(caught.getMessage());
				LoadingWidget.getInstance().hide();
			}
			@Override
			public void onSuccess(GwtRpcResponseList<Query> result) {
				String selected = (select == null ? null : select.toString());
				if (selected == null && iQuerySelector.getWidget().getSelectedIndex() >= 0) {
					selected = iQuerySelector.getWidget().getValue(iQuerySelector.getWidget().getSelectedIndex());
				}
				iQuerySelector.getWidget().clear();
				if (result.isEmpty()) {
					iHeader.setErrorMessage(MESSAGES.errorNoReportsAvailable());
				} else {
					iQuerySelector.getWidget().addItem(MESSAGES.itemSelect(), "-1");
					iQueries = result;
					for (int i = 0; i < result.size(); i++) {
						iQuerySelector.getWidget().addItem(result.get(i).getName(), result.get(i).getId().toString());
						if (selected != null && selected.equals(result.get(i).getId().toString()))
							iQuerySelector.getWidget().setSelectedIndex(1 + i);
					}
					queryChanged();
				}
				LoadingWidget.getInstance().hide();
				if (reload) reload(History.getToken());
			}
		});
	}
	
	private void queryChanged() {
		iHeader.clearMessage();
		if (iQuerySelector.getWidget().getSelectedIndex() <= 0) {
			iHeader.setEnabled("execute", false);
			iHeader.setEnabled("edit", false);
			iDescription.setHTML("");
			for (int i = 0; i < iOptions.size(); i++)
				iForm.getRowFormatter().setVisible(3 + i, false);
		} else {
			iHeader.setEnabled("execute", true);
			Long id = Long.valueOf(iQuerySelector.getWidget().getValue(iQuerySelector.getWidget().getSelectedIndex()));
			for (SavedHQLInterface.Query q: iQueries) {
				if (id.equals(q.getId())) {
					iDescription.setHTML(q.getDescription());
					iHeader.setEnabled("edit", iHeader.isEnabled("add"));
					for (int i = 0; i < iOptions.size(); i++) {
						SavedHQLInterface.Option option = iOptions.get(i);
						iForm.getRowFormatter().setVisible(3 + i, q.getQuery().contains("%" + option.getType() + "%"));
					}
				}
			}
		}
	}
	
	public static int compare(String[] a, String[] b, int col) {
		for (int i = 0; i < a.length; i++) {
			int c = (col + i) % a.length;
			try {
				int cmp = Double.valueOf(a[c] == null ? "0" : a[c]).compareTo(Double.valueOf(b[c] == null ? "0" : b[c]));
				if (cmp != 0) return cmp;
			} catch (NumberFormatException e) {
				int cmp = (a[c] == null ? "" : a[c]).compareTo(b[c] == null ? "" : b[c]);
				if (cmp != 0) return cmp;
			}
		}
		return 0;
	}
	
	public void populate(Table result) {
		if (result == null || result.size() <= 1) {
			iTableHeader.setMessage(MESSAGES.errorNoResults());
			iTableHeader.setEnabled("next", false);
			iTableHeader.setEnabled("previous", false);
			iTableFooter.setVisible(false);
		} else {
			for (int i = 0; i < result.size(); i++) {
				String[] row = result.get(i);
				List<Widget> line = new ArrayList<Widget>();
				if (i == 0) {
					iFirstField = row[0];
					for (String x: row) {
						final String name = x.replace('_', ' ').trim();
						final UniTimeTableHeader h = new UniTimeTableHeader(name, 1);
						final int col = line.size();
						h.addOperation(new UniTimeTableHeader.Operation() {
							@Override
							public void execute() {
								iTable.sort(col, new Comparator<String[]>() {
									@Override
									public int compare(String[] o1, String[] o2) {
										return SavedHQLPage.compare(o1, o2, col);
									}
								});
								iLastSort = (h.getOrder() != null && h.getOrder() ? (1 + col) : -1 - col);
								History.newItem(iLastHistory + ":" + iFirstLine + ":" + iLastSort, false);
								setBack();
							}
							@Override
							public boolean isApplicable() {
								return true;
							}
							@Override
							public boolean hasSeparator() {
								return false;
							}
							@Override
							public String getName() {
								return MESSAGES.opSortBy(name);
							}
						});
						line.add(h);
					}
				} else {
					for (String x: row) {
						line.add(new HTML(x == null ? "" : x.replace("\\n", "<br>")));
					}
				}
				iTable.addRow(i == 0 ? null : row, line);
			}
			if (iFirstField != null && iFirstField.startsWith("__"))
				iTable.setColumnVisible(0, false);
			iHeader.setEnabled("print", true);
			iHeader.setEnabled("export", iTable.getRowCount() > 1);
			if (result.size() == 102)
				iTableHeader.setMessage(MESSAGES.infoShowingLines(iFirstLine + 1, iFirstLine + 100));
			else if (iFirstLine > 0)
				iTableHeader.setMessage(MESSAGES.infoShowingLines(iFirstLine + 1, iFirstLine + result.size() - 1));
			else 
				iTableHeader.setMessage(MESSAGES.infoShowingAllLines(result.size() - 1));
			iTableHeader.setEnabled("next", result.size() > 101);
			iTableHeader.setEnabled("previous", iFirstLine > 0);
			iTableFooter.setVisible(iFirstLine > 0 || result.size() > 101);
			if (iLastSort != 0) {
				iTable.sort(iTable.getHeader(Math.abs(iLastSort) - 1), new Comparator<String[]>() {
					@Override
					public int compare(String[] o1, String[] o2) {
						return SavedHQLPage.compare(o1, o2, Math.abs(iLastSort) - 1);
					}
				}, iLastSort > 0);
			}
		}
		setBack();
	}
	
	public void setBack() {
		if (iFirstField == null || !iFirstField.startsWith("__") || iTable.getRowCount() <= 1) return;
		HQLSetBackRpcRequest request = new HQLSetBackRpcRequest();
		for (int i = 1; i < iTable.getRowCount(); i++) {
			String[] row = iTable.getData(i);
			if (row != null) {
				Long id = Long.valueOf(row[0]);
				request.addId(id);
			}
		}
		request.setAppearance(iAppearance);
		request.setHistory(History.getToken());
		request.setType(iFirstField);
		RPC.execute(request, new AsyncCallback<GwtRpcResponseNull>() {
			@Override
			public void onFailure(Throwable caught) {
				iHeader.setErrorMessage(caught.getMessage());
			}

			@Override
			public void onSuccess(GwtRpcResponseNull result) {
			}
		});
	}
	
	public void reload(String history) {
		if (history == null) return;
		if (history.indexOf('&') >= 0)
			history = history.substring(0, history.indexOf('&')); 
		if (history.isEmpty()) return;
		String[] params = history.split(":");
		Long id = Long.valueOf(params[0]);
		SavedHQLInterface.Query query = null;
		for (int i = 0; i < iQueries.size(); i++) {
			SavedHQLInterface.Query q = iQueries.get(i);
			if (id.equals(q.getId())) {
				query = q;
				iQuerySelector.getWidget().setSelectedIndex(1 + i);
				queryChanged();
				break;
			}
		}
		if (query == null) return;
		int idx = 1;
		for (int i = 0; i < iOptions.size(); i++) {
			SavedHQLInterface.Option option = iOptions.get(i);
			if (query.getQuery().contains("%" + option.getType() + "%")) {
				String param = params[idx++];
				if (param == null || param.isEmpty()) continue;
				ListBox list = ((UniTimeWidget<ListBox>)iForm.getWidget(3 + i, 1)).getWidget();
				if (list.isMultipleSelect()) {
					for (int j = 0; j < list.getItemCount(); j++) {
						String value = list.getValue(j);
						boolean contains = false;
						for (String o: param.split(",")) if (o.equals(value)) { contains = true; break; }
						list.setItemSelected(j, contains);
					}
				} else {
					for (int j = 1; j < list.getItemCount(); j++) {
						if (list.getValue(j).equals(param)) {
							list.setSelectedIndex(j); break;
						}
					}
				}
			}
		}
		iFirstLine = Integer.parseInt(params[idx++]);
		iLastSort = Integer.parseInt(params[idx++]);
		execute();
	}
	
	private void execute() {
		HQLExecuteRpcRequest request = new HQLExecuteRpcRequest();
		iHeader.setEnabled("print", false);
		iHeader.setEnabled("export", false);

		Long id = Long.valueOf(iQuerySelector.getWidget().getValue(iQuerySelector.getWidget().getSelectedIndex()));
		for (SavedHQLInterface.Query q: iQueries) {
			if (id.equals(q.getId())) {
				request.setQuery(q); break;
			}
		}
		if (request.getQuery() == null) {
			iHeader.setErrorMessage(MESSAGES.errorNoReportSelected());
			return;
		}
		iLastHistory = request.getQuery().getId().toString();
		for (int i = 0; i < iOptions.size(); i++) {
			SavedHQLInterface.Option option = iOptions.get(i);
			if (request.getQuery().getQuery().contains("%" + option.getType() + "%")) {
				SavedHQLInterface.IdValue o = new SavedHQLInterface.IdValue();
				o.setValue(option.getType());
				ListBox list = ((UniTimeWidget<ListBox>)iForm.getWidget(3 + i, 1)).getWidget();
				String value = "";
				boolean allSelected = true;
				if (list.isMultipleSelect()) {
					for (int j = 0; j < list.getItemCount(); j++)
						if (list.isItemSelected(j)) {
							if (!value.isEmpty()) value += ",";
							value += list.getValue(j);
						} else {
							allSelected = false;
						}
				} else if (list.getSelectedIndex() > 0) {
					value = list.getValue(list.getSelectedIndex());
				}
				if (value.isEmpty()) {
					iHeader.setErrorMessage(MESSAGES.errorItemNotSelected(option.getName()));
					return;
				}
				request.addOption(option.getType(), value);
				iLastHistory += ":" + (list.isMultipleSelect() && allSelected ? "" : value);
			}
		}
		
		iTable.clearTable(); iFirstField = null;
		iTableHeader.clearMessage();
		iHeader.clearMessage();
		LoadingWidget.getInstance().show(MESSAGES.waitExecuting(request.getQuery().getName()));
		History.newItem(iLastHistory + ":" + iFirstLine + ":" + iLastSort, false);
		request.setFromRow(iFirstLine);
		request.setMaxRows(101);
		RPC.execute(request, new AsyncCallback<Table>() {
			@Override
			public void onFailure(Throwable caught) {
				iTableHeader.setErrorMessage(caught.getMessage());
				LoadingWidget.getInstance().hide();
			}

			@Override
			public void onSuccess(Table result) {
				populate(result);
				LoadingWidget.getInstance().hide();
			}
		});		
	}
	
}
