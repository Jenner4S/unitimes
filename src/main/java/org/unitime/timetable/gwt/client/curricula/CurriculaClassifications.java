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
package org.unitime.timetable.gwt.client.curricula;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.unitime.timetable.gwt.client.widgets.UniTimeTable;
import org.unitime.timetable.gwt.client.widgets.UniTimeTextBox;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.shared.CurriculumInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.AcademicClassificationInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.CurriculumClassificationInterface;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ValueBoxBase;

/**
 * @author Tomas Muller
 */
public class CurriculaClassifications extends Composite {
	protected static final GwtMessages MESSAGES = GWT.create(GwtMessages.class);
	
	private UniTimeTable iTable;

	private List<AcademicClassificationInterface> iClassifications = null;
	private List<ExpectedChangedHandler> iExpectedChangedHandlers = new ArrayList<ExpectedChangedHandler>();
	private List<NameChangedHandler> iNameChangedHandlers = new ArrayList<NameChangedHandler>();
	
	public CurriculaClassifications() {
		iTable = new UniTimeTable();
		iTable.setCellPadding(2);
		iTable.setCellSpacing(0);
		initWidget(iTable);
	}
	
	public List<AcademicClassificationInterface> getClassifications() {
		return iClassifications;
	}
	
	public void setup(List<AcademicClassificationInterface> classifications) {
		iTable.clear(true);
		iClassifications = classifications;
		iTable.setText(0, 0, MESSAGES.propName());
		iTable.setText(1, 0, MESSAGES.propClassification());
		iTable.setText(2, 0, MESSAGES.propLastLikeEnrollment());
		iTable.getCellFormatter().setWordWrap(2, 0, false);
		iTable.setText(3, 0, MESSAGES.propProjectedByRule());
		iTable.getCellFormatter().setWordWrap(3, 0, false);
		iTable.setText(4, 0, MESSAGES.propRequestedEnrollment());
		iTable.getCellFormatter().setWordWrap(4, 0, false);
		iTable.setText(5, 0, MESSAGES.propCurrentEnrollment());
		iTable.getCellFormatter().setWordWrap(5, 0, false);
		iTable.setText(6, 0, MESSAGES.propCourseRequests());
		iTable.getCellFormatter().setWordWrap(6, 0, false);
		iTable.setText(7, 0, MESSAGES.propSnapshotRequestedEnrollment());
		iTable.getCellFormatter().setWordWrap(7, 0, false);
		iTable.setText(8, 0, MESSAGES.propSnapshotProjectedByRule());
		iTable.getCellFormatter().setWordWrap(8, 0, false);
		int col = 0;
		for (final AcademicClassificationInterface clasf: iClassifications) {
			col ++;
			final UniTimeTextBox name = new UniTimeTextBox(6, ValueBoxBase.TextAlignment.RIGHT, true);
			name.setText(clasf.getCode());
			name.setWidth("60px");
			name.setMaxLength(20);
			final int xcol = col - 1;
			name.addChangeHandler(new ChangeHandler() {
				@Override
				public void onChange(ChangeEvent event) {
					NameChangedEvent e = new NameChangedEvent(clasf, xcol, name.getText());
					for (NameChangedHandler h: iNameChangedHandlers)
						h.nameChanged(e);
				}
			});
			iTable.setWidget(0, col, name);
			iTable.getCellFormatter().setHorizontalAlignment(0, col, HasHorizontalAlignment.ALIGN_CENTER);
			
			iTable.setWidget(1, col, new HTML(clasf.getName().replace(" ", "<br>")));
			iTable.getCellFormatter().setHorizontalAlignment(1, col, HasHorizontalAlignment.ALIGN_CENTER);
			
			UniTimeTextBox ll = new UniTimeTextBox(6, ValueBoxBase.TextAlignment.RIGHT, false);
			iTable.setWidget(2, col, ll);
			iTable.getCellFormatter().setHorizontalAlignment(2, col, HasHorizontalAlignment.ALIGN_CENTER);
			
			UniTimeTextBox proj = new UniTimeTextBox(6, ValueBoxBase.TextAlignment.RIGHT, false);
			iTable.setWidget(3, col, proj);
			iTable.getCellFormatter().setHorizontalAlignment(3, col, HasHorizontalAlignment.ALIGN_CENTER);
			
			final UniTimeTextBox expected = new UniTimeTextBox(6, ValueBoxBase.TextAlignment.RIGHT, true);
			iTable.setWidget(4, col, expected);
			iTable.getCellFormatter().setHorizontalAlignment(4, col, HasHorizontalAlignment.ALIGN_CENTER);
			expected.addChangeHandler(new ChangeHandler() {
				@Override
				public void onChange(ChangeEvent event) {
					Integer exp = null;
					if (!expected.getText().isEmpty()) {
						try {
							exp = Integer.parseInt(expected.getText());
							if (exp < 0) {
								exp = null;
								expected.setText(null);
							}
						} catch (Exception e) {
							expected.setText(null);
						}
					}
					ExpectedChangedEvent e = new ExpectedChangedEvent(clasf, xcol, exp);
					for (ExpectedChangedHandler h: iExpectedChangedHandlers)
						h.expectedChanged(e);
				}
			});
			
			UniTimeTextBox enrl = new UniTimeTextBox(6, ValueBoxBase.TextAlignment.RIGHT, false);
			iTable.setWidget(5, col, enrl);
			iTable.getCellFormatter().setHorizontalAlignment(5, col, HasHorizontalAlignment.ALIGN_CENTER);

			UniTimeTextBox req = new UniTimeTextBox(6, ValueBoxBase.TextAlignment.RIGHT, false);
			iTable.setWidget(6, col, req);
			iTable.getCellFormatter().setHorizontalAlignment(6, col, HasHorizontalAlignment.ALIGN_CENTER);

			UniTimeTextBox ssExp = new UniTimeTextBox(6, ValueBoxBase.TextAlignment.RIGHT, false);
			iTable.setWidget(7, col, ssExp);
			iTable.getCellFormatter().setHorizontalAlignment(7, col, HasHorizontalAlignment.ALIGN_CENTER);

			UniTimeTextBox ssProj = new UniTimeTextBox(6, ValueBoxBase.TextAlignment.RIGHT, false);
			iTable.setWidget(8, col, ssProj);
			iTable.getCellFormatter().setHorizontalAlignment(8, col, HasHorizontalAlignment.ALIGN_CENTER);
		}
	}
	
	public void populate(TreeSet<CurriculumClassificationInterface> classifications) {
		int col = 0;
		for (AcademicClassificationInterface clasf: iClassifications) {
			CurriculumClassificationInterface ci = null;
			if (classifications != null && !classifications.isEmpty())
				for (CurriculumClassificationInterface x: classifications) {
					if (x.getAcademicClassification().getId().equals(clasf.getId())) { ci = x; break; }
				}
			if (ci == null) {
				setName(col, clasf.getCode());
				setExpected(col, null);
				setEnrollment(col, null);
				setLastLike(col, null);
				setProjection(col, null);
				setRequested(col, null);
				setSnapshotExpected(col, null);
				setSnapshotProjection(col, null);
			} else {
				setName(col, ci.getName());
				setExpected(col, ci.getExpected());
				setEnrollment(col, ci.getEnrollment());
				setLastLike(col, ci.getLastLike());
				setProjection(col, ci.getProjection());
				setRequested(col, ci.getRequested());
				setSnapshotExpected(col, (!ci.isSessionHasSnapshotData() ? null : ci.getSnapshotExpected()));
				setSnapshotProjection(col, (!ci.isSessionHasSnapshotData() ? null : ci.getSnapshotProjection()));
			}
			col++;
		}
	}
	
	public boolean saveCurriculum(CurriculumInterface c) {
		if (c.hasClassifications()) c.getClassifications().clear();
		int col = 0;
		for (AcademicClassificationInterface ac: iClassifications) {
			Integer exp = getExpected(col);
			if (exp != null) {
				CurriculumClassificationInterface clasf = new CurriculumClassificationInterface();
				clasf.setAcademicClassification(ac);
				clasf.setCurriculumId(c.getId());
				clasf.setExpected(exp);
				clasf.setLastLike(getLastLike(col));
				clasf.setProjection(getProjection(col));
				clasf.setName(getName(col));
				if (c.isSessionHasSnapshotData()){
					clasf.setSnapshotExpected(getSnapshotExpected(col));
					clasf.setSnapshotProjection(getSnapshotProjection(col));
				}
				c.addClassification(clasf);
			}
			col ++;
		}
		return true;
	}
	
	public void setEnabled(boolean enabled) {
		for (int i = 0; i < iClassifications.size(); i++) {
			((UniTimeTextBox)iTable.getWidget(0, 1 + i)).setReadOnly(!enabled);
			((UniTimeTextBox)iTable.getWidget(4, 1 + i)).setReadOnly(!enabled);
			boolean visible = enabled || getExpected(i) != null || getEnrollment(i) != null || getLastLike(i) != null || getProjection(i) != null || getRequested(i) != null || getSnapshotExpected(i) != null || getSnapshotProjection(i) != null;
			for (int j = 0; j < 8; j++)
				iTable.getWidget(j, 1 + i).setVisible(visible);
		}
	}
	
	public int getColumn(AcademicClassificationInterface classification) {
		for (int i = 0; i < iClassifications.size(); i++)
			if (classification.getId().equals(iClassifications.get(i).getId()))
				return i;
		return -1;
	}
	
	public String getName(int column) {
		return ((TextBox)iTable.getWidget(0, 1 + column)).getText();
	}
	
	public void setName(int column, String name) {
		((TextBox)iTable.getWidget(0, 1 + column)).setText(name);
	}

	public Integer getExpected(int column) {
		String text = ((TextBox)iTable.getWidget(4, 1 + column)).getText();
		if (text.isEmpty()) return null;
		try {
			return Integer.parseInt(text);
		} catch (Exception e) {
			return null;
		}
	}
	
	public void setExpected(int column, Integer expected) {
		((TextBox)iTable.getWidget(4, 1 + column)).setText(expected == null ? "" : expected.toString());
	}

	
	public Integer getEnrollment(int column) {
		String text = ((TextBox)iTable.getWidget(5, 1 + column)).getText();
		if (text.isEmpty()) return null;
		try {
			return Integer.parseInt(text);
		} catch (Exception e) {
			return null;
		}
	}

	public void setEnrollment(int column, Integer enrollment) {
		((TextBox)iTable.getWidget(5, 1 + column)).setText(enrollment == null || enrollment == 0 ? "" : enrollment.toString());
	}
	
	public Integer getLastLike(int column) {
		String text = ((TextBox)iTable.getWidget(2, 1 + column)).getText();
		if (text.isEmpty()) return null;
		try {
			return Integer.parseInt(text);
		} catch (Exception e) {
			return null;
		}
	}

	public void setLastLike(int column, Integer lastLike) {
		((TextBox)iTable.getWidget(2, 1 + column)).setText(lastLike == null || lastLike == 0? "" : lastLike.toString());
	}

	public Integer getProjection(int column) {
		String text = ((TextBox)iTable.getWidget(3, 1 + column)).getText();
		if (text.isEmpty()) return null;
		try {
			return Integer.parseInt(text);
		} catch (Exception e) {
			return null;
		}
	}

	public void setProjection(int column, Integer projection) {
		((TextBox)iTable.getWidget(3, 1 + column)).setText(projection == null || projection == 0 ? "" : projection.toString());
	}
	
	public Integer getRequested(int column) {
		String text = ((TextBox)iTable.getWidget(6, 1 + column)).getText();
		if (text.isEmpty()) return null;
		try {
			return Integer.parseInt(text);
		} catch (Exception e) {
			return null;
		}
	}
	
	public void setRequested(int column, Integer requested) {
		((TextBox)iTable.getWidget(6, 1 + column)).setText(requested == null || requested == 0 ? "" : requested.toString());
	}

	public Integer getSnapshotExpected(int column) {
		String text = ((TextBox)iTable.getWidget(7, 1 + column)).getText();
		if (text.isEmpty()) return null;
		try {
			return Integer.parseInt(text);
		} catch (Exception e) {
			return null;
		}
	}

	public void setSnapshotExpected(int column, Integer expected) {
		((TextBox)iTable.getWidget(7, 1 + column)).setText(expected == null || expected == 0 ? "" : expected.toString());
	}
	
	public Integer getSnapshotProjection(int column) {
		String text = ((TextBox)iTable.getWidget(8, 1 + column)).getText();
		if (text.isEmpty()) return null;
		try {
			return Integer.parseInt(text);
		} catch (Exception e) {
			return null;
		}
	}

	public void setSnapshotProjection(int column, Integer snapshotProjection) {
		((TextBox)iTable.getWidget(8, 1 + column)).setText(snapshotProjection == null || snapshotProjection == 0 ? "" : snapshotProjection.toString());
	}

	public static class ExpectedChangedEvent {
		private AcademicClassificationInterface iClassification;
		private int iColumn;
		private Integer iExpected;
		
		ExpectedChangedEvent(AcademicClassificationInterface classification, int column, Integer expected) {
			iClassification = classification;
			iColumn = column;
			iExpected = expected;
		}
		
		public AcademicClassificationInterface getClassification() { return iClassification; }
		public int getColumn() { return iColumn; }
		public Integer getExpected() { return iExpected; }
	}
	
	public static interface ExpectedChangedHandler {
		public void expectedChanged(ExpectedChangedEvent e);
	}
	
	public void addExpectedChangedHandler(ExpectedChangedHandler h) {
		iExpectedChangedHandlers.add(h);
	}
	
	public static class NameChangedEvent {
		private AcademicClassificationInterface iClassification;
		private int iColumn;
		private String iName;
		
		NameChangedEvent(AcademicClassificationInterface classification, int column, String name) {
			iClassification = classification;
			iColumn = column;
			iName = name;
		}
		
		public AcademicClassificationInterface getClassification() { return iClassification; }
		public int getColumn() { return iColumn; }
		public String getName() { return iName; }
	}
	
	public static interface NameChangedHandler {
		public void nameChanged(NameChangedEvent e);
	}
	
	public void addNameChangedHandler(NameChangedHandler h) {
		iNameChangedHandlers.add(h);
	}
	
	public void setVisible(int col, boolean visible) {
		for (int row = 0; row < iTable.getRowCount(); row++)
			iTable.getCellFormatter().setVisible(row, 1 + col, visible);
	}
	
	public void hideEmptyColumns() {
		for (int i = 0; i < iClassifications.size(); i++) {
			setVisible(i, getExpected(i) != null || getLastLike(i) != null || getEnrollment(i) != null || getProjection(i) != null || getRequested(i) != null || getSnapshotExpected(i) != null || getSnapshotProjection(i) != null);
		}
	}
	
	public void hideEmptyRows() {
		boolean last = false, proj = false, enrl = false, req = false, ssExp = false, ssProj = false;
		for (int i = 0; i < iClassifications.size(); i++) {
			if (getLastLike(i) != null) last = true;
			if (getProjection(i) != null) proj = true;
			if (getEnrollment(i) != null) enrl = true;
			if (getRequested(i) != null) req = true;
			if (getSnapshotExpected(i) != null) ssExp = true;
			if (getSnapshotProjection(i) != null) ssProj = true;
		}
		iTable.getRowFormatter().setVisible(2, last);
		iTable.getRowFormatter().setVisible(3, proj);
		iTable.getRowFormatter().setVisible(5, enrl);
		iTable.getRowFormatter().setVisible(6, req);
		iTable.getRowFormatter().setVisible(7, ssExp);
		iTable.getRowFormatter().setVisible(8, ssProj);
	}
	
	public void showAllColumns() {
		for (int i = 0; i < iClassifications.size(); i++) {
			setVisible(i, true);
		}
	}
}
