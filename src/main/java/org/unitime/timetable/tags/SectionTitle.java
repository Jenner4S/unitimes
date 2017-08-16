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
package org.unitime.timetable.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * @author Tomas Muller
 */
public class SectionTitle  extends BodyTagSupport {
	
	private static final long serialVersionUID = 2869346521706583988L;

	public int doStartTag() throws JspException {
		return EVAL_BODY_BUFFERED;
	}
	
	public int doEndTag() throws JspException {
		if (getParent()!=null && getParent() instanceof SectionHeader) {
			((SectionHeader)getParent()).setTitle(getBodyContent().getString());
		} else {
			try {
				String body = (getBodyContent()==null?null:getBodyContent().getString());
				if (body==null || body.trim().length()==0) {
					pageContext.getOut().println("<DIV class='WelcomeRowHeadBlank'>&nbsp;</DIV>");
				} else {
					pageContext.getOut().println("<DIV class='WelcomeRowHead'>");
					pageContext.getOut().println(body);
					pageContext.getOut().println("</DIV>");
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new JspTagException(e.getMessage());
			}
		}
		return EVAL_PAGE;
	}	

}
