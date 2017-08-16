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
package org.unitime.timetable.gwt.client.widgets;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Tomas Muller
 */
public class SimpleForm extends FlexTable {
	private int iColSpan;
	
	public SimpleForm(int colSpan) {
		super();
		iColSpan = colSpan;
		setStylePrimaryName("unitime-MainTable");
		addStyleName("unitime-NotPrintableBottomLine");
		setCellPadding(2);
		setCellSpacing(0);
	}
	
	public SimpleForm() {
		this(2);
	}
	
	public void setColSpan(int colSpan) { iColSpan = colSpan; }
	
	public int getColSpan() { return iColSpan; }
	
	public int addHeaderRow(Widget widget) {
		int row = getRowCount();
		getFlexCellFormatter().setColSpan(row, 0, iColSpan);
		getFlexCellFormatter().setStyleName(row, 0, "unitime-MainTableHeader");
		getRowFormatter().setStyleName(row, "unitime-MainTableHeaderRow");
		setWidget(row, 0, widget);
		return row;
	}
	
	public int addHeaderRow(String text) {
		return addHeaderRow(new Label(text, false));
	}

	public int addRow(Widget widget) {
		int row = getRowCount();
		getFlexCellFormatter().setColSpan(row, 0, iColSpan);
		if (widget instanceof HasMobileScroll) {
			ScrollPanel scroll = new ScrollPanel(widget);
			scroll.addStyleName("table-row");
			setWidget(row, 0, scroll);
		} else {
			setWidget(row, 0, widget);
		}
		return row;
	}
	
	protected int addBottomRow(Widget widget, boolean printable) {
		int row = getRowCount();
		getFlexCellFormatter().setColSpan(row, 0, iColSpan);
		getFlexCellFormatter().setStyleName(row, 0, "unitime-MainTableBottomHeader");
		if (!printable)
			getFlexCellFormatter().addStyleName(row, 0, "unitime-NoPrint");
		getFlexCellFormatter().addStyleName(row, 0, "unitime-TopLine");
		removeStyleName("unitime-NotPrintableBottomLine");
		setWidget(row, 0, widget);
		return row;
	}
	
	public int addBottomRow(Widget widget) {
		return addBottomRow(widget, true);
	}

	public int addNotPrintableBottomRow(Widget widget) {
		return addBottomRow(widget, false);
	}
	
	public int addRow(String text, Widget widget) {
		return addRow(text, widget, iColSpan - 1);
	}

	public int addRow(String text, Widget widget, int colspan) {
		return addRow(new Label(text), widget, colspan);
	}
	
	public int addRow(Widget header, Widget widget) {
		return addRow(header, widget, iColSpan - 1);
	}
	
	public int addRow(Widget header, Widget widget, int colSpan) {
		header.addStyleName("label-cell");
		int row = getRowCount();
		setWidget(row, 0, header);
		getCellFormatter().setStyleName(row, 0, "label-td");
		if (widget instanceof HasMobileScroll) {
			ScrollPanel scroll = new ScrollPanel(widget);
			scroll.addStyleName("table-cell");
			setWidget(row, 1, scroll);
			getCellFormatter().setStyleName(row, 1, "table-td");
		} else {
			widget.addStyleName("widget-cell");
			setWidget(row, 1, widget);
			getCellFormatter().setStyleName(row, 1, "widget-td");
		}
		if (colSpan != 1)
			getFlexCellFormatter().setColSpan(row, 1, colSpan);
		if (header.getElement().getId() == null || header.getElement().getId().isEmpty())
			header.getElement().setId(DOM.createUniqueId());
		if (widget instanceof UniTimeWidget)
			Roles.getTextboxRole().setAriaLabelledbyProperty(((UniTimeWidget)widget).getWidget().getElement(), Id.of(header.getElement()));
		else
			Roles.getTextboxRole().setAriaLabelledbyProperty(widget.getElement(), Id.of(header.getElement()));
		return row;
	}
	
	public int getRow(String text) {
		for (int row = 0; row < getRowCount(); row ++) {
			if (getCellCount(row) > 1) {
				Widget w = getWidget(row, 0);
				if (w instanceof HasText && text.equals(((HasText)w).getText())) return row;
			}
		}
		return -1;
	}
	
	@Override
	public void clear() {
		for (int row = getRowCount() - 1; row >= 0; row--)
			removeRow(row);
	}

	public int getRowForWidget(Widget w) {
		for (Element td = w.getElement(); td != null; td = DOM.getParent(td)) {
			if (td.getPropertyString("tagName").equalsIgnoreCase("td")) {
				Element tr = DOM.getParent(td);
				Element body = DOM.getParent(tr);
				if (body == getBodyElement())
					return DOM.getChildIndex(body, tr);
			}
			if (td == getBodyElement()) { return -1; }
		}
		return -1;
	}
	
	public static interface HasMobileScroll {}
}
