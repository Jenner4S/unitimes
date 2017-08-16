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
package org.unitime.timetable.webutil.timegrid;

import java.awt.Color;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.StringTokenizer;

import org.unitime.localization.impl.Localization;
import org.unitime.timetable.gwt.resources.GwtConstants;
import org.unitime.timetable.model.PreferenceLevel;
import org.unitime.timetable.util.Constants;
import org.unitime.timetable.util.Formats;
import org.unitime.timetable.util.PdfEventHandler;
import org.unitime.timetable.util.PdfFont;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;


/**
 * @author Tomas Muller
 */
public class PdfTimetableGridTable {
	protected static final GwtConstants CONSTANTS = Localization.create(GwtConstants.class);
	protected static Formats.Format<Date> sDF = Formats.getDateFormat(Formats.Pattern.DATE_EVENT_SHORT);
	protected static Formats.Format<Number> sUtF = Formats.getNumberFormat("0.00");
	private TimetableGridTable iTable = null;
	private PdfWriter iWriter = null;
	private Document iDocument = null;
	private PdfPTable iPdfTable = null;
	private int iDay = 0;
	
	protected PdfTimetableGridTable(TimetableGridTable table) {
		iTable = table;
	}
	
	public static void export2Pdf(TimetableGridTable table, OutputStream out) throws Exception {
		PdfTimetableGridTable x = new PdfTimetableGridTable(table);
		x.export(out);
	}
	
	public void export(OutputStream out) throws Exception {
		int nrCols = getNrColumns();
		iDocument = (iTable.isDispModePerWeekVertical() ?
				new Document(new Rectangle(60.0f+7.0f*nrCols,60.0f+9.3f*nrCols), 30, 30, 30, 30)
			:
				new Document(new Rectangle(60.0f+10.0f*nrCols,60.0f+7.5f*nrCols), 30, 30, 30, 30)
			);

		iWriter = PdfEventHandler.initFooter(iDocument, out);
		iDocument.open();
	
		if (iTable.isDispModeInRow()) {
			for (iDay=iTable.startDay();iDay<=iTable.endDay();iDay++) {
				if (iTable.skipDay(iDay)) continue;
				int rowNumber=0; 
	        	for (Enumeration e = iTable.models().elements(); e.hasMoreElements(); rowNumber++) {
	        		printToPdf((TimetableGridModel)e.nextElement(),rowNumber);
	        	}
	        	flushTable();
			}
		} else {
			int rowNumber=0; 
			for (Enumeration e = iTable.models().elements(); e.hasMoreElements(); rowNumber++) {
				printToPdf((TimetableGridModel)e.nextElement(),rowNumber);
			}
		}
	
		printLegend();
    
		iDocument.close();
	}
	
	public int getNrColumns() {
		return 12 + (1+iTable.lastSlot()-iTable.firstSlot());
		/*
		int nrCols = (iTable.isDispModePerWeekVertical()?1:12);
		if (iTable.isDispModePerWeekVertical()) {
			for (int day=iTable.startDay(); day<=iTable.endDay(); day++) 
				nrCols++;
		} else {//isDispModeInRow() || isDispModePerWeekVertical()
			for (int day=iTable.startDay();(iTable.isDispModeInRow() && day<=iTable.endDay()) || (iTable.isDispModePerWeek() && day==iTable.startDay());day++) {
				for (int slot=iTable.firstSlot();slot<=iTable.lastSlot();slot+=iTable.nrSlotsPerPeriod()) {
					nrCols+=iTable.nrSlotsPerPeriod();
				}
			}
		}
		return nrCols;
		*/
	}
	
	private static Color sBorderColor = new Color(100,100,100);
	
	
	public PdfPCell createCell() {
		PdfPCell cell = new PdfPCell();
		cell.setBorderColor(sBorderColor);
		cell.setPadding(3);
		cell.setBorderWidth(0);
		cell.setVerticalAlignment(Element.ALIGN_TOP);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setBorderWidthTop(1);
		cell.setBorderWidthBottom(1);
		cell.setBorderWidthLeft(1);
		cell.setBorderWidthRight(1);
		return cell;
	}
	
	public PdfPCell createCellNoBorder() {
		PdfPCell cell = new PdfPCell();
		cell.setBorderColor(sBorderColor);
		cell.setPadding(3);
		cell.setBorderWidth(0);
		cell.setVerticalAlignment(Element.ALIGN_TOP);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		return cell;
	}

	public void addText(PdfPCell cell, String text) {
		if (text==null) return;
		addText(cell, text, false);
	}
	
	public void addText(PdfPCell cell, String text, boolean bold) {
		if (text==null) return;
        if (text.indexOf("<span")>=0)
            text = text.replaceAll("</span>","").replaceAll("<span .*>", "");
		if (cell.getPhrase()==null) {
			cell.setPhrase(new Paragraph(text, PdfFont.getSmallFont(bold)));
			cell.setVerticalAlignment(Element.ALIGN_TOP);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		} else {
			cell.getPhrase().add(new Chunk("\n"+text, PdfFont.getSmallFont(bold)));
		}
	}
	
	public void addTextVertical(PdfPCell cell, String text) throws Exception  {
		if (text==null) return;
		addTextVertical(cell, text, false);
	}
	
	public void addTextVertical(PdfPCell cell, String text, boolean bold) throws Exception  {
		if (text==null) return;
        if (text.indexOf("<span")>=0)
            text = text.replaceAll("</span>","").replaceAll("<span .*>", "");
        Font font = PdfFont.getFont(bold);
		BaseFont bf = font.getBaseFont();
		float width = bf.getWidthPoint(text, font.getSize());
		PdfTemplate template = iWriter.getDirectContent().createTemplate(2 * font.getSize() + 4, width);
		template.beginText();
		template.setColorFill(Color.BLACK);
		template.setFontAndSize(bf, font.getSize());
		template.setTextMatrix(0, 2);
		template.showText(text);
		template.endText();
		template.setWidth(width);
		template.setHeight(font.getSize() + 2);
		//make an Image object from the template
		Image img = Image.getInstance(template);
		img.setRotationDegrees(270);
		//embed the image in a Chunk
		Chunk ck = new Chunk(img, 0, 0);
		
		if (cell.getPhrase()==null) {
			cell.setPhrase(new Paragraph(ck));
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		} else {
			cell.getPhrase().add(ck);
		}
	}
	
	public void printHeader(TimetableGridModel model, int rowNumber) throws Exception {
		if (iTable.isDispModePerWeekVertical()) {
			for (int slot=iTable.lastSlot();slot>=iTable.firstSlot();slot-=iTable.nrSlotsPerPeriod()) {
				int time = (slot-iTable.nrSlotsPerPeriod()+1)*Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN;
				PdfPCell c = createCell();
				c.setColspan(iTable.nrSlotsPerPeriod());
				if (slot<iTable.lastSlot()) c.setBorderWidthLeft(0);
				addTextVertical(c, Constants.toTime(time), true);
				iPdfTable.addCell(c);
			}
			PdfPCell c = createCell();
			c.setColspan(12);
			c.setBorderWidthLeft(0);
			if (model.getResourceType() == TimetableGridModel.sResourceTypeRoom)
				addTextVertical(c, "(" + model.getSize() + ", " + sUtF.format(model.getUtilization() / iTable.nrSlotsPerPeriod()) + ")");
			else if (model.getResourceType() == TimetableGridModel.sResourceTypeCurriculum)
				addTextVertical(c, "(" + model.getSize() + ")");
			else if (model.getResourceType() == TimetableGridModel.sResourceTypeStudentGroup)
				addTextVertical(c, "(" + model.getSize() + ", " + sUtF.format(100.0 * model.getUtilization()) + "%)");
			else if (model.getSize() > 0)
				addTextVertical(c, "(" + model.getSize() + ")");
			addTextVertical(c, model.getName(), true);
			iPdfTable.addCell(c);
		} else {
			PdfPCell c = createCell();
			c.setColspan(12);
			if (iTable.isDispModeInRow())
				addText(c, CONSTANTS.days()[iDay], true);
			else {
				addText(c, model.getName(), true);
				if (model.getResourceType() == TimetableGridModel.sResourceTypeRoom)
					addText(c, "(" + model.getSize() + ", " + sUtF.format(model.getUtilization() / iTable.nrSlotsPerPeriod()) + ")");
				else if (model.getResourceType() == TimetableGridModel.sResourceTypeCurriculum)
					addText(c, "(" + model.getSize() + ")");
				else if (model.getResourceType() == TimetableGridModel.sResourceTypeStudentGroup)
					addText(c, "(" + model.getSize() + ", " + sUtF.format(100.0 * model.getUtilization()) + "%)");
				else if (model.getSize() > 0)
					addText(c, "(" + model.getSize() + ")");
			}
			iPdfTable.addCell(c);
			for (int slot=iTable.firstSlot();slot<=iTable.lastSlot();slot+=iTable.nrSlotsPerPeriod()) {
				int time = slot*Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN;
				c = createCell();
				c.setBorderWidthLeft(0);
				c.setColspan(iTable.nrSlotsPerPeriod());
				addText(c, Constants.toTime(time), true);
				iPdfTable.addCell(c);
			}
		}
		iPdfTable.setHeaderRows(1);
	}
	
	private Color getColor(String rgbColor) {
		StringTokenizer x = new StringTokenizer(rgbColor.substring("rgb(".length(),rgbColor.length()-")".length()),",");
		return new Color(
				Integer.parseInt(x.nextToken()),
				Integer.parseInt(x.nextToken()),
				Integer.parseInt(x.nextToken()));
	}
	
	private void createTable() {
		iPdfTable = new PdfPTable(getNrColumns());
		iPdfTable.setWidthPercentage(100);
		iPdfTable.getDefaultCell().setPadding(3);
		iPdfTable.getDefaultCell().setBorderWidth(1);
		iPdfTable.setSplitRows(false);
		iPdfTable.setSpacingBefore(10);
		if (iTable.isDispModePerWeek()) {
			iPdfTable.setKeepTogether(true);
		}
	}
	
	private void flushTable() throws Exception {
		iDocument.add(iPdfTable);
	}
	
	public void printToPdf(TimetableGridModel model, int rowNumber) throws Exception {
		model.clearRendered();
		
		if (iTable.isDispModePerWeek()) {
			createTable();
			printHeader(model, 0);
		} else {
			if (rowNumber==0) {
				createTable();
				printHeader(model, 0);
			}
		}
		
		if (iTable.isDispModeInRow()) {
			int maxIdx = model.getMaxIdxForDay(iDay,iTable.firstSlot(),iTable.lastSlot());
			for (int idx=0;idx<=maxIdx;idx++) {
				PdfPCell c = createCell();
				c.setBorderWidthTop(0);
				if (idx==0)
					addText(c, model.getName()+(model.getSize()>0?" ("+model.getSize()+")":""), true);
				c.setBorderWidthBottom(idx==maxIdx?1:0);
				c.setColspan(12);
				iPdfTable.addCell(c);
				for (int slot=iTable.firstSlot();slot<=iTable.lastSlot();slot++) {
					int slotsToEnd = iTable.lastSlot()-slot+1;
					TimetableGridCell cell = model.getCell(iDay,slot,idx);
                    int length = (cell==null?1:cell.getLength()+cell.getSlot()-slot);
            		int colSpan = (cell==null?1:Math.min(length,slotsToEnd));
					if (cell==null) {
						String bgColor = model.getBackground(iDay,slot);
						if (bgColor==null && !model.isAvailable(iDay,slot))
							bgColor=TimetableGridCell.sBgColorNotAvailable;
						c = createCell();
						c.setBorderWidthTop(0);
						c.setBorderWidthBottom(idx==maxIdx?1:model.getCell(iDay,slot,idx+1)!=null?1:0);
						c.setBorderWidthLeft(0);
						boolean eod = (slot == iTable.lastSlot());
						boolean in = !eod && model.getCell(iDay,slot+1,idx)==null && ((slot+1-iTable.firstSlot())%iTable.nrSlotsPerPeriod())!=0;
						c.setBorderWidthRight(eod || !in?1:0);
						c.setColspan(colSpan);
						if (bgColor!=null)
							c.setBackgroundColor(getColor(bgColor));
						iPdfTable.addCell(c);
					} else {
						String bgColor = cell.getBackground();
						if (iTable.getBgMode()==TimetableGridModel.sBgModeNone) {
							for (int i=0;i<length;i++)
								if (!model.isAvailable(iDay,slot+i)) {
									bgColor = TimetableGridCell.sBgColorNotAvailableButAssigned;
	                    			break;
                    			}
                		}
						c = createCell();
						c.setBorderWidthTop(0);
						c.setBorderWidthLeft(0);
						c.setColspan(colSpan);
						c.setBackgroundColor(getColor(bgColor));
						addText(c, cell.getName());
						if (iTable.getShowTimes())
							addText(c, cell.getTime());
						if (iTable.getResourceType()!=TimetableGridModel.sResourceTypeRoom)
							addText(c, cell.getRoomName());
						if (iTable.getResourceType()!=TimetableGridModel.sResourceTypeInstructor && iTable.getShowInstructors() && !cell.getInstructor().isEmpty())
							addText(c, cell.getInstructor());
						if (iTable.getShowComments())
							addText(c, cell.getShortCommentNoColors()==null?null:cell.getShortCommentNoColors());
						if (iTable.getWeek()==-100 && cell.hasDays() && !cell.getDays().equals(iTable.getDefaultDatePatternName()))
							addText(c, cell.getDays());
						iPdfTable.addCell(c);
						slot+=length-1;
					}
				}
			}	
		} else  if (iTable.isDispModePerWeekHorizontal()) {
			for (int day=iTable.startDay();day<=iTable.endDay();day++) {
				if (iTable.skipDay(day)) continue;
				int maxIdx = model.getMaxIdxForDay(day,iTable.firstSlot(),iTable.lastSlot());
				for (int idx=0;idx<=maxIdx;idx++) {
					PdfPCell c = createCell();
					c.setBorderWidthTop(0);
					if (idx==0)
						addText(c, CONSTANTS.days()[day], true);
					c.setBorderWidthBottom(idx==maxIdx?1:0);
					c.setColspan(12);
					iPdfTable.addCell(c);
					for (int slot=iTable.firstSlot();slot<=iTable.lastSlot();slot++) {
						int slotsToEnd = iTable.lastSlot()-slot+1;
						TimetableGridCell cell = model.getCell(day,slot,idx);
                        int length = (cell==null?1:cell.getLength()+cell.getSlot()-slot);
	            		int colSpan = (cell==null?1:Math.min(length,slotsToEnd));
						if (cell==null) {
							String bgColor = model.getBackground(day,slot);
							if (bgColor==null && !model.isAvailable(day,slot))
								bgColor=TimetableGridCell.sBgColorNotAvailable;
							c = createCell();
							c.setBorderWidthTop(0);
							c.setBorderWidthBottom(idx==maxIdx?1:model.getCell(day,slot,idx+1)!=null?1:0);
							c.setBorderWidthLeft(0);
							boolean eod = (slot == iTable.lastSlot());
							boolean in = !eod && model.getCell(day,slot+1,idx)==null && ((slot+1-iTable.firstSlot())%iTable.nrSlotsPerPeriod())!=0;
							c.setBorderWidthRight(eod || !in?1:0);
							iPdfTable.addCell(c);
						} else {
							String bgColor = cell.getBackground();
	                		if (iTable.getBgMode()==TimetableGridModel.sBgModeNone) {
	                    		for (int i=0;i<length;i++)
	                    			if (!model.isAvailable(day,slot+i)) {
	                    				bgColor = TimetableGridCell.sBgColorNotAvailableButAssigned;
	                    				break;
	                    			}
	                		}
							c = createCell();
							c.setBorderWidthTop(0);
							c.setBorderWidthLeft(0);
							c.setColspan(colSpan);
							if (bgColor!=null)
								c.setBackgroundColor(getColor(bgColor));
							addText(c, cell.getName());
							if (iTable.getShowTimes())
								addText(c, cell.getTime());
							if (iTable.getResourceType()!=TimetableGridModel.sResourceTypeRoom)
								addText(c, cell.getRoomName());
							if (iTable.getResourceType()!=TimetableGridModel.sResourceTypeInstructor && iTable.getShowInstructors() && !cell.getInstructor().isEmpty())
								addText(c, cell.getInstructor());
							if (iTable.getShowComments())
								addText(c, cell.getShortCommentNoColors()==null?null:cell.getShortCommentNoColors());
							if (iTable.getWeek()==-100 && cell.hasDays() && !cell.getDays().equals(iTable.getDefaultDatePatternName()))
								addText(c, cell.getDays());
							iPdfTable.addCell(c);
							slot+=length-1;
						}
					}
				}
			}
		} else  if (iTable.isDispModeWeekByWeekHorizontal()) {
			Calendar cal = Calendar.getInstance(Locale.US);
			cal.setTime(iTable.iFirstDate);
			for (int d = 0; d < 365; d++ ) {
				if (d > 0)
					cal.add(Calendar.DAY_OF_YEAR, 1);
				int date = d + iTable.iFirstDay;
				if (model.getFirstDay() >= 0 && (date < model.getFirstDay() || date > model.getFirstDay() + 6)) continue;
				int day = d % 7;
				if (day < iTable.startDay() || day > iTable.endDay() || iTable.skipDay(day)) continue;
				boolean hasClasses = false;
				for (int slot=iTable.firstSlot();slot<=iTable.lastSlot();slot++) {
					if (model.getCell(day, slot, 0, date) != null) {
						hasClasses = true; break;
					}
				}
				if (!hasClasses) continue;
				int maxIdx = model.getMaxIdxForDay(day,iTable.firstSlot(),iTable.lastSlot(),date);
				for (int idx=0;idx<=maxIdx;idx++) {
					PdfPCell c = createCell();
					c.setBorderWidthTop(0);
					if (idx==0)
						addText(c, CONSTANTS.days()[day]+" "+sDF.format(cal.getTime()), true);
					c.setBorderWidthBottom(idx==maxIdx?1:0);
					c.setColspan(12);
					iPdfTable.addCell(c);
					for (int slot=iTable.firstSlot();slot<=iTable.lastSlot();slot++) {
						int slotsToEnd = iTable.lastSlot()-slot+1;
						TimetableGridCell cell = model.getCell(day,slot,idx,date);
                        int length = (cell==null?1:cell.getLength()+cell.getSlot()-slot);
	            		int colSpan = (cell==null?1:Math.min(length,slotsToEnd));
						if (cell==null) {
							String bgColor = model.getBackground(day,slot);
							if (bgColor==null && !model.isAvailable(day,slot))
								bgColor=TimetableGridCell.sBgColorNotAvailable;
							c = createCell();
							c.setBorderWidthTop(0);
							c.setBorderWidthBottom(idx==maxIdx?1:model.getCell(day,slot,idx+1,date)!=null?1:0);
							c.setBorderWidthLeft(0);
							boolean eod = (slot == iTable.lastSlot());
							boolean in = !eod && model.getCell(day,slot+1,idx,date)==null && ((slot+1-iTable.firstSlot())%iTable.nrSlotsPerPeriod())!=0;
							c.setBorderWidthRight(eod || !in?1:0);
							iPdfTable.addCell(c);
						} else {
							String bgColor = cell.getBackground();
	                		if (iTable.getBgMode()==TimetableGridModel.sBgModeNone) {
	                    		for (int i=0;i<length;i++)
	                    			if (!model.isAvailable(day,slot+i)) {
	                    				bgColor = TimetableGridCell.sBgColorNotAvailableButAssigned;
	                    				break;
	                    			}
	                		}
							c = createCell();
							c.setBorderWidthTop(0);
							c.setBorderWidthLeft(0);
							c.setColspan(colSpan);
							if (bgColor!=null)
								c.setBackgroundColor(getColor(bgColor));
							addText(c, cell.getName());
							if (iTable.getShowTimes())
								addText(c, cell.getTime());
							if (iTable.getResourceType()!=TimetableGridModel.sResourceTypeRoom)
								addText(c, cell.getRoomName());
							if (iTable.getResourceType()!=TimetableGridModel.sResourceTypeInstructor && iTable.getShowInstructors() && !cell.getInstructor().isEmpty())
								addText(c, cell.getInstructor());
							if (iTable.getShowComments())
								addText(c, cell.getShortCommentNoColors()==null?null:cell.getShortCommentNoColors());
							iPdfTable.addCell(c);
							slot+=length-1;
						}
					}
				}
			}
		} else { //isDispModePerWeekVertical
			for (int day=iTable.startDay();day<=iTable.endDay();day++) {
				if (iTable.skipDay(day)) continue;
				int maxIdx = model.getMaxIdxForDay(day,iTable.firstSlot(),iTable.lastSlot());
				for (int idx=0;idx<=maxIdx;idx++) {
					PdfPCell c = null;
					for (int slot=iTable.lastSlot(); slot>=iTable.firstSlot();slot--) {
						int slotsToEnd = slot+1 - iTable.firstSlot();
						TimetableGridCell cell = model.getCell(day,slot,idx);
                        int length = (cell==null?1:1+slot-cell.getSlot());
						int colSpan = (cell==null?1:Math.min(length,slotsToEnd));
						if (cell==null) {
							String bgColor = model.getBackground(day,slot);
							if (bgColor==null && !model.isAvailable(day,slot))
								bgColor=TimetableGridCell.sBgColorNotAvailable;
							c = createCell();
							c.setBorderWidthTop(0);
							c.setMinimumHeight(100f);
							c.setBorderWidthBottom(idx==maxIdx?1:model.getCell(day,slot,idx+1)!=null?1:0);
							c.setBorderWidthLeft(slot==iTable.lastSlot()?1:0);
							boolean eod = (slot == iTable.firstSlot());
							boolean in = !eod && model.getCell(day,slot-1,idx)==null && ((slot-iTable.firstSlot())%iTable.nrSlotsPerPeriod())!=0;
							c.setBorderWidthRight(eod || !in?1:0);
							iPdfTable.addCell(c);
						} else {
							String bgColor = cell.getBackground();
	                		if (iTable.getBgMode()==TimetableGridModel.sBgModeNone) {
	                    		for (int i=0;i<length;i++)
	                    			if (!model.isAvailable(day,slot-i)) {
	                    				bgColor = TimetableGridCell.sBgColorNotAvailableButAssigned;
	                    				break;
	                    			}
	                		}
							c = createCell();
							c.setBorderWidthTop(0);
							c.setBorderWidthLeft(slot==iTable.lastSlot()?1:0);
							c.setColspan(colSpan);
							if (bgColor!=null)
								c.setBackgroundColor(getColor(bgColor));
							if (iTable.getWeek()==-100 && cell.hasDays() && !cell.getDays().equals(iTable.getDefaultDatePatternName()))
								addTextVertical(c, cell.getDays());
							if (iTable.getShowTimes())
								addText(c, cell.getTime());
							if (iTable.getResourceType()!=TimetableGridModel.sResourceTypeRoom)
								addTextVertical(c, cell.getRoomName());
							if (iTable.getResourceType()!=TimetableGridModel.sResourceTypeInstructor && iTable.getShowInstructors() && !cell.getInstructor().isEmpty())
								addTextVertical(c, cell.getInstructor());
							if (iTable.getShowComments())
								addTextVertical(c, cell.getShortCommentNoColors()==null?null:cell.getShortCommentNoColors());
							addTextVertical(c, cell.getName());
							iPdfTable.addCell(c);
							slot-=length-1;
						}
					}
					c = createCell();
					c.setBorderWidthTop(0);
					c.setBorderWidthLeft(0);
					if (idx==0)
						addTextVertical(c, CONSTANTS.days()[day], true);
					c.setBorderWidthBottom(idx==maxIdx?1:0);
					c.setColspan(12);
					iPdfTable.addCell(c);
				}
			}
			/*
			int step = iTable.nrSlotsPerPeriod();
			for (int slot=iTable.firstSlot();slot<=iTable.lastSlot();slot+=step) {
				int time = slot * Constants.SLOT_LENGTH_MIN + Constants.FIRST_SLOT_TIME_MIN;
                int slotsToEnd = iTable.lastSlot()-slot+1;
                if ((slot%iTable.nrSlotsPerPeriod()) == 0) {
    				c = createCell("TimetableHeadCell"+(slot==iTable.firstSlot()?"":"In")+"Vertical");
    				addText(c, Constants.toTime(time), true);
    				iPdfTable.addCell(c);
                } else {
                	c = createCell("TimetableHeadCellInVertical");
                	iPdfTable.addCell(c);
                }
                for (int day=iTable.startDay();day<=iTable.endDay();day++) {
                	int maxIdx = model.getMaxIdxForDay(day,iTable.firstSlot(),iTable.lastSlot());
                	for (int idx=0;idx<=maxIdx;idx++) {
                    	TimetableGridCell cell = model.getCell(day,slot, idx);
                    	if (model.isRendered(day,slot,idx)) continue;
						int rowSpan = (cell==null?1:Math.min(cell.getLength()+cell.getSlot()-slot,slotsToEnd));
						int colSpan = (iTable.getResourceType()==TimetableGridModel.sResourceTypeDepartment && cell!=null?1:model.getDepth(day,slot,idx,maxIdx,rowSpan)); 
						model.setRendered(day,slot,idx,colSpan,rowSpan);
						int rowSpanDivStep = (int)Math.ceil(((double)rowSpan)/step);
                    	
                    	if (cell==null) {
							String bgColor = model.getBackground(day,slot);
							if (bgColor==null && !model.isAvailable(day,slot))
								bgColor=TimetableGridCell.sBgColorNotAvailable;
                            boolean eol = (day==iTable.endDay() && (idx+colSpan-1)==maxIdx);
							c = createCell("TimetableCell"+(slot==iTable.firstSlot()?"":"In")+"Vertical"+(eol?"EOL":""));
							c.setColspan(colSpan);
							//c.setRowspan(rowSpanDivStep);
							if (bgColor!=null)
								c.setBackgroundColor(getColor(bgColor));
							iPdfTable.addCell(c);
                    	} else {
                    		String bgColor = cell.getBackground();
                    		if (iTable.getBgMode()==TimetableGridModel.sBgModeNone) {
                        		for (int i=0;i<cell.getLength();i++)
                        			if (!model.isAvailable(day,slot+i)) {
                        				bgColor = TimetableGridCell.sBgColorNotAvailableButAssigned;
                        				break;
                        			}
                    		}
                    		boolean eol = (day==iTable.endDay());
                    		c = createCell("TimetableCell"+(slot==iTable.firstSlot()?"":"In")+"Vertical" + (eol?"EOL":""));
							c.setColspan(colSpan);
							//c.setRowspan(rowSpanDivStep);
							if (bgColor!=null)
								c.setBackgroundColor(getColor(bgColor));
							addText(c, cell.getName());
							if (iTable.getResourceType()!=TimetableGridModel.sResourceTypeRoom)
								addText(c, cell.getRoomName());
							if (iTable.getShowComments())
								addText(c, cell.getShortComment()==null?"":cell.getShortComment());
							if (iTable.getWeek()==-100 && cell.hasDays() && !cell.getDays().equals(iTable.getDefaultDatePatternName()))
								addText(c, cell.getDays());
							iPdfTable.addCell(c);
                    	}
                    }
                }
			}
			*/
		}
		
		if (iTable.isDispModePerWeek()) {
			flushTable();
		}
	}
	
	private void addLegendRow(String color, String text) {
		PdfPCell c = createCellNoBorder();
		c.setBorderWidth(1);
		c.setBackgroundColor(getColor(color));
		iPdfTable.addCell(c);
		c = createCellNoBorder();
		addText(c, "  "+text);
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		iPdfTable.addCell(c);
	}
	
	public void printLegend() throws Exception {
		iPdfTable = new PdfPTable(2);
		iPdfTable.setWidths(new float[] {10f,200f});
		iPdfTable.getDefaultCell().setPadding(3);
		iPdfTable.getDefaultCell().setBorderWidth(1);
		iPdfTable.setHorizontalAlignment(Element.ALIGN_LEFT);
		iPdfTable.setSplitRows(false);
		iPdfTable.setSpacingBefore(10);
		iPdfTable.setKeepTogether(true);
		
		if (iTable.getBgMode()!=TimetableGridModel.sBgModeNone) {
			PdfPCell c = createCellNoBorder();
			c.setColspan(2);
			addText(c,"Assigned classes:");
			c.setHorizontalAlignment(Element.ALIGN_LEFT);
			iPdfTable.addCell(c);
		}
        if (iTable.getBgMode()==TimetableGridModel.sBgModeTimePref) {
        	addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sRequired), "Required time"); 
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sStronglyPreferred), "Strongly preferred time");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sPreferred), "Preferred time");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sNeutral), "No time preference");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sDiscouraged), "Discouraged time");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sStronglyDiscouraged), "Strongly discouraged time");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sProhibited), "Prohibited time");
        } else if (iTable.getBgMode()==TimetableGridModel.sBgModeRoomPref) {
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sRequired), "Required room");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sStronglyPreferred), "Strongly preferred room");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sPreferred), "Preferred room");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sNeutral), "No room preference");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sDiscouraged), "Discouraged room");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sStronglyDiscouraged), "Strongly discouraged room");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sProhibited), "Prohibited room");
        } else if (iTable.getBgMode()==TimetableGridModel.sBgModeStudentConf) {
            for (int nrConflicts=0;nrConflicts<=15;nrConflicts++) {
                String color = TimetableGridCell.conflicts2color(nrConflicts);
                addLegendRow(color, ""+nrConflicts+" "+(nrConflicts==15?"or more ":"")+"student conflicts");
            }
        } else if (iTable.getBgMode()==TimetableGridModel.sBgModeInstructorBtbPref) {
        	addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sNeutral), "No instructor back-to-back preference (distance=0)");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sDiscouraged), "Discouraged back-to-back (0<distance<=5)");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sStronglyDiscouraged), "Strongly discouraged back-to-back (5<distance<=20)");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sProhibited), "Prohibited back-to-back (20<distance)");
        } else if (iTable.getBgMode()==TimetableGridModel.sBgModeDistributionConstPref) {
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sNeutral), "No violated constraint(distance=0)");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sDiscouraged), "Discouraged/preferred constraint violated");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sStronglyDiscouraged), "Strongly discouraged/preferred constraint violated");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sProhibited), "Required/prohibited constraint violated");
        } else if (iTable.getBgMode()==TimetableGridModel.sBgModePerturbations) {
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sStronglyPreferred), "No change");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sNeutral), "No initial assignment");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sDiscouraged), "Room changed");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sStronglyDiscouraged), "Time changed");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sProhibited), "Both time and room changed");
        } else if (iTable.getBgMode()==TimetableGridModel.sBgModePerturbationPenalty) {
            for (int nrConflicts=0;nrConflicts<=15;nrConflicts++) {
                String color = TimetableGridCell.conflicts2color(nrConflicts);
                addLegendRow(color, ""+(nrConflicts==0?"Zero perturbation penalty":nrConflicts==15?"Perturbation penalty above 15":"Perturbation penalty below or equal to "+nrConflicts)+"");
            }
        } else if (iTable.getBgMode()==TimetableGridModel.sBgModeHardConflicts) {
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sRequired), "Required time and room");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sStronglyPreferred), "Can be moved in room with no hard conflict");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sPreferred), "Can be moved in room (but there is a hard conflict), can be moved in time with no conflict");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sNeutral), "Can be moved in room (but there is a hard conflict)");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sDiscouraged), "Can be moved in time with no hard conflict, cannot be moved in room");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sStronglyDiscouraged), "Can be moved in time (but there is a hard conflict), cannot be moved in room");
        } else if (iTable.getBgMode()==TimetableGridModel.sBgModeDepartmentalBalancing) {
            for (int nrConflicts=0;nrConflicts<=3;nrConflicts++) {
                String color = TimetableGridCell.conflicts2colorFast(nrConflicts);
                addLegendRow(color, ""+(nrConflicts==0?"Zero penalty":nrConflicts==3?"Penalty equal or above 3":"Penalty equal to "+nrConflicts)+"");
            }
        } else if (iTable.getBgMode()==TimetableGridModel.sBgModeTooBigRooms) {
        	addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sRequired), "Assigned room is smaller than room limit of a class");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sNeutral), "Assigned room is not more than 25% bigger than the smallest avaialable room");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sDiscouraged), "Assigned room is not more than 50% bigger than the smallest avaialable room");
            addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sStronglyDiscouraged), "Assigned room is more than 50% bigger than the smallest avaialable room");
        } else if (iTable.getBgMode()==TimetableGridModel.sBgModeStudentGroups) {
        	addLegendRow(TimetableGridCell.percentage2color(100), "All students of the group are in this class");
        	for (int p = 95; p >= 5; p -= 5)
        		addLegendRow(TimetableGridCell.percentage2color(p), "More than " + p + "% students of the group are in this class");
        	addLegendRow(TimetableGridCell.percentage2color(0), "Less than 5% students of the group are in this class");
        } 
		PdfPCell c = createCellNoBorder();
		c.setColspan(2);
		addText(c,"Free times:");
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		iPdfTable.addCell(c);
        addLegendRow(TimetableGridCell.sBgColorNotAvailable, "Time not available");
        addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sNeutral), "No preference");
        if (iTable.getShowUselessTimes()) {
        	addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sDiscouraged), "Standard (MWF or TTh) time pattern is broken (time cannot be used for MW, WF, MF or TTh class)");
        	addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sStronglyDiscouraged), "Useless half-hour");
        	addLegendRow(TimetableGridCell.pref2color(PreferenceLevel.sProhibited), "Useless half-hour and broken standard time pattern");
        }
        
        if (iTable.isDispModePerWeekVertical())
        	iDocument.newPage();
        
        iDocument.add(iPdfTable);
    }
}
