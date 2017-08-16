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
package org.unitime.timetable.filter;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.tiles.ComponentDefinition;
import org.apache.struts.tiles.TilesUtil;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.unitime.localization.impl.Localization;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.shared.PageAccessException;
import org.unitime.timetable.security.UserContext;
import org.unitime.timetable.security.rights.Right;


/**
 * @author Tomas Muller
 */
public class PageAccessFilter implements Filter {
	protected static GwtMessages MESSAGES = Localization.create(GwtMessages.class);
	private static Log sLog = LogFactory.getLog(PageAccessFilter.class);
	private static DecimalFormat sDF = new DecimalFormat("0.00");
	private ServletContext iContext;
	private Hashtable<String, String> iPath2Tile = new Hashtable<String, String>();
	private long debugTime = 30000; // Print info about the page if the page load took at least this time.
	private long dumpTime = 300000; // Print debug info about the page if the page load took at least this time.
	private boolean dumpSessionAttribues = false; // Include session attributes in the dump.
	
	public void init(FilterConfig cfg) throws ServletException {
		iContext = cfg.getServletContext();
		try {
			Document config = (new SAXReader()).read(cfg.getServletContext().getResource(cfg.getInitParameter("config")));
			for (Iterator i=config.getRootElement().element("action-mappings").elementIterator("action"); i.hasNext();) {
				Element action = (Element)i.next();
				String path = action.attributeValue("path");
				String input = action.attributeValue("input");
				if (path!=null && input!=null) {
					iPath2Tile.put(path+".do", input);
				}
			}
		} catch (Exception e) {
			sLog.error("Unable to read config "+cfg.getInitParameter("config")+", reason: "+e.getMessage());
		}
		if (cfg.getInitParameter("debug-time")!=null) {
			debugTime = Long.parseLong(cfg.getInitParameter("debug-time"));
		}
		if (cfg.getInitParameter("dump-time")!=null) {
			dumpTime = Long.parseLong(cfg.getInitParameter("dump-time"));
		}
		if (cfg.getInitParameter("session-attributes")!=null) {
			dumpSessionAttribues = Boolean.parseBoolean(cfg.getInitParameter("session-attributes"));
		}
	}
	
	private UserContext getUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserContext)
			return (UserContext)authentication.getPrincipal();
		return null;
	}
	
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain ) throws IOException, ServletException {
		try {
			long t0 = System.currentTimeMillis();
			
			UserContext user = getUser();
			if (user != null)
				ApplicationProperties.setSessionId(user.getCurrentAcademicSessionId());
			
			if (request instanceof HttpServletRequest) {
				HttpServletRequest r = (HttpServletRequest)request;
				if (r.getRequestURI().endsWith(".do")) {
					HttpServletResponse x = (HttpServletResponse)response;
					String def = r.getRequestURI().substring(r.getContextPath().length());
					try {
						if (iPath2Tile.containsKey(def)) {
							String tile = iPath2Tile.get(def);
							ComponentDefinition c = TilesUtil.getDefinition(tile, request, iContext);
							HttpSession s = r.getSession();
							if (c!=null && "true".equals(c.getAttribute("checkLogin"))) {
								if (user == null) {
									sLog.warn("Page "+r.getRequestURI()+" denied: user not logged in");
									if (s.isNew()) 
										x.sendRedirect(x.encodeURL(r.getContextPath()+"/loginRequired.do?message=" + MESSAGES.authenticationExpired()));
									else
										x.sendRedirect(x.encodeURL(r.getContextPath()+"/loginRequired.do?message=" + MESSAGES.authenticationRequired()));
									return;
								}
							}
							if (c!=null && "true".equals(c.getAttribute("checkRole"))) {
								if (user == null || user.getCurrentAuthority() == null || !user.getCurrentAuthority().hasRight(Right.HasRole)) {
									sLog.warn("Page "+r.getRequestURI()+" denined: no role");
									x.sendRedirect(x.encodeURL(r.getContextPath()+"/loginRequired.do?message=" + MESSAGES.authenticationInsufficient()));
									return;
								}
							}
							if (c!=null && "true".equals(c.getAttribute("checkAdmin"))) {
								if (user == null || user.getCurrentAuthority() == null || !user.getCurrentAuthority().hasRight(Right.IsAdmin)) {
									sLog.warn("Page "+r.getRequestURI()+" denied: user not admin");
									x.sendRedirect(x.encodeURL(r.getContextPath()+"/loginRequired.do?message=" + MESSAGES.authenticationInsufficient()));
									return;
								}
							}
							/*
							if (c!=null && "true".equals(c.getAttribute("checkAccessLevel"))) {
								String appAccess = (String) s.getAttribute(Constants.SESSION_APP_ACCESS_LEVEL);
								if (appAccess!=null && !"true".equalsIgnoreCase(appAccess)) {
									sLog.warn("Page "+r.getRequestURI()+" denied: application access disabled");
									x.sendRedirect(x.encodeURL(r.getContextPath()+"/loginRequired.do?message=The+application+is+temporarily+unavailable.+Please+try+again+after+some+time."));
									return;
								}
							}
							*/
						}
					} catch (Exception e) {
						sLog.warn("Unable to check page access for "+r.getRequestURI()+", reason: "+e.getMessage(), e);
					}
				}
			}
			
			// Process request
			Throwable exception = null;
			try {
				chain.doFilter(request,response);
			} catch (Throwable t) {
				exception = t;
			}
			
			long t1 = System.currentTimeMillis(); 
			if (request instanceof HttpServletRequest && ((t1-t0)>debugTime || exception!=null)) {
				HttpServletRequest r = (HttpServletRequest)request;
				String message = "Page "+r.getRequestURI()+" took "+sDF.format((t1-t0)/1000.0)+" s.";
				if (exception!=null) {
					message = exception+" seen on page "+r.getRequestURI()+" (page took "+sDF.format((t1-t0)/1000.0)+" s).";
				}
				if (exception!=null || (t1-t0)>dumpTime) {
					UserContext u = null;
					try {
						u = getUser();
					} catch (IllegalStateException e) {}
					if (u==null) {
						message += "\n  User: no user";
					} else {
						message += "\n  User: " + u.getUsername() + (u.getCurrentAuthority() != null ? " ("+u.getCurrentAuthority()+")" : "");
					}
					message += "\n  Request parameters:";
					for (Enumeration e=r.getParameterNames(); e.hasMoreElements();) {
						String n = (String)e.nextElement();
						if ("password".equals(n)) continue;
						message+="\n    "+n+"="+r.getParameter(n);
					}
					try {
						if (dumpSessionAttribues && r.getSession() != null) {
							message += "\n  Session attributes:";
							for (Enumeration e=r.getSession().getAttributeNames(); e.hasMoreElements();) {
								String n = (String)e.nextElement();
								message+="\n    "+n+"="+r.getSession().getAttribute(n);
							}
						}
					} catch (IllegalStateException e) {
						message += "\n    INVALID SESSION";
					}
				} else {
					UserContext u = getUser();
					if (u==null) {
						message += "  (User: no user)";
					} else {
						message += "  (User: " + u.getUsername() + (u.getCurrentAuthority() != null ? " ("+u.getCurrentAuthority()+")" : "");
					}
				}
				if (exception!=null)
					sLog.warn(message);
				else
					sLog.info(message);
			}		

			if (exception!=null) {
				if (exception instanceof PageAccessException && request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
					HttpServletRequest r = (HttpServletRequest)request;
					HttpServletResponse x = (HttpServletResponse)response;
					String message = exception.getMessage();
					if (message == null || message.isEmpty()) {
						HttpSession s = r.getSession();
						if (getUser() == null) {
							if (s.isNew()) 
								message = MESSAGES.authenticationExpired();
							else
								message = MESSAGES.authenticationRequired();
						} else {
							message = MESSAGES.authenticationInsufficient();
						}
					}
					x.sendRedirect(x.encodeURL(r.getContextPath() + "/loginRequired.do?message=" + message));
				} else if (exception instanceof ServletException) {
					throw (ServletException)exception;
				} else  if (exception instanceof IOException) {
					throw (IOException)exception;
				} else if (exception instanceof RuntimeException) {
					throw (RuntimeException)exception;
				} else {
					throw new ServletException(exception);
				}
			}
		
		} finally {
			ApplicationProperties.setSessionId(null);
		}
	}

	public void destroy() {
	}
}
