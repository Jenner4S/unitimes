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
package org.unitime.timetable.gwt.client.page;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.unitime.timetable.gwt.client.Client.GwtPageChangeEvent;
import org.unitime.timetable.gwt.client.Client.GwtPageChangedHandler;
import org.unitime.timetable.gwt.client.aria.AriaStatus;
import org.unitime.timetable.gwt.client.Client;
import org.unitime.timetable.gwt.client.ToolBox;
import org.unitime.timetable.gwt.command.client.GwtRpcException;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ScrollEvent;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * @author Tomas Muller
 */
public class UniTimeNotifications {
	protected static Display NOTIFICATIONS = GWT.create(Display.class);
	protected static Display NOTIFICATIONS_MOBILE = GWT.create(MobileDisplay.class);
	protected static Logger sLogger = Logger.getLogger(UniTimeNotifications.class.getName());
	
	protected static Display getDisplay() {
		if (Window.getClientWidth() <= 800)
			return NOTIFICATIONS_MOBILE;
		else
			return NOTIFICATIONS;
	}

	public static void info(String text) {
		AriaStatus.getInstance().setText(text);
		sLogger.log(Level.FINEST, text);
		getDisplay().addNotification(text, NotificationType.INFO);
	}
	
	public static void warn(String text) {
		AriaStatus.getInstance().setText(text);
		sLogger.log(Level.FINER, text);
		getDisplay().addNotification(text, NotificationType.WARN);
	}
	
	public static void error(Throwable t) {
		if (t == null) return;
		Throwable u = ToolBox.unwrap(t);
		error(u.getMessage(), u);
	}

	public static void error(String text, Throwable t) {
		AriaStatus.getInstance().setText(text);
		if (t == null) {
			sLogger.log(Level.FINE, text);
		} else {
			t = ToolBox.unwrap(t);
			if (t instanceof GwtRpcException) {
				GwtRpcException e = (GwtRpcException)t;
				if (e.hasCause())
					sLogger.log(Level.WARNING, text, e.getCause());
				else
					sLogger.log(Level.INFO, text);
			} else {
				sLogger.log(Level.SEVERE, text, t);
			}
		}
		getDisplay().addNotification(text, NotificationType.ERROR);
	}
	
	public static void error(String text) {
		error(text, null);
	}

	public static native void createTriggers()/*-{
		$wnd.gwtShowMessage = function(message) {
			@org.unitime.timetable.gwt.client.page.UniTimeNotifications::info(Ljava/lang/String;)(message);
		};
		$wnd.gwtShowWarning = function(message) {
			@org.unitime.timetable.gwt.client.page.UniTimeNotifications::warn(Ljava/lang/String;)(message);
		};
		$wnd.gwtShowError = function(message) {
			@org.unitime.timetable.gwt.client.page.UniTimeNotifications::error(Ljava/lang/String;)(message);
		};
	}-*/;
	
	public static enum NotificationType {
		INFO,
		WARN,
		ERROR;
	}
	
	public static interface Display {
		public void addNotification(String html, NotificationType type);		
	}
	
	public static interface MobileDisplay extends Display {}
	
	public static class Implementation implements Display {
		protected List<Notification> iNotifications = new ArrayList<Notification>();
		protected Timer iMoveTimer = null;
		protected Animation iAnimation;
		
		public Implementation() {
			Window.addResizeHandler(new ResizeHandler() {
				@Override
				public void onResize(ResizeEvent event) {
					delayedMove();
				}
			});
			Window.addWindowScrollHandler(new Window.ScrollHandler() {
				@Override
				public void onWindowScroll(ScrollEvent event) {
					delayedMove();
				}
			});
			Client.addGwtPageChangedHandler(new GwtPageChangedHandler() {
				@Override
				public void onChange(GwtPageChangeEvent event) {
					delayedMove();
				}
			});
			iMoveTimer = new Timer() {
				@Override
				public void run() {
					move();
				}
			};
			iAnimation = new NotificationAnimation();		
		}
		
		protected void addNotification(final Notification notification) {
			RootPanel.get().add(notification, Window.getScrollLeft() + Window.getClientWidth() - 445, Window.getScrollTop() + Window.getClientHeight());
			iAnimation.cancel();
			for (Iterator<Notification> i = iNotifications.iterator(); i.hasNext(); ) {
				Notification n = i.next();
				if (n.equals(notification)) {
					n.hide(); i.remove();
				}
			}
			move();
			iNotifications.add(0, notification);
			iAnimation.run(1000);
			Timer timer = new Timer() {
				@Override
				public void run() {
					notification.hide();
					iNotifications.remove(notification);
				}
			};
			notification.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					notification.hide();
					iNotifications.remove(notification);
					move();
				}
			});
			timer.schedule(10000);
		}
		
		protected void move() {
			int height = 0;
			iAnimation.cancel();
			for (HTML notification: iNotifications) {
				height += notification.getElement().getClientHeight() + 10;
				notification.getElement().getStyle().setProperty("left", (Window.getScrollLeft() + Window.getClientWidth() - 445) + "px");
				notification.getElement().getStyle().setProperty("top", (Window.getScrollTop() + Window.getClientHeight() - height) + "px");
			}
		}
		
		protected void delayedMove() {
			if (!iNotifications.isEmpty())
				iMoveTimer.schedule(100);
		}
		

		@Override
		public void addNotification(String html, NotificationType type) {
			switch (type) {
			case ERROR:
				addNotification(new Notification(html, "unitime-NotificationError"));
				break;
			case WARN:
				addNotification(new Notification(html, "unitime-NotificationWarning"));
				break;
			case INFO:
				addNotification(new Notification(html, "unitime-NotificationInfo"));
				break;
			}
		}
		
		private class NotificationAnimation extends Animation {
			@Override
			protected void onUpdate(double progress) {
				if (iNotifications.isEmpty()) return;
				int height = - (int) Math.round((1.0 - progress) * (iNotifications.get(0).getElement().getClientHeight() + 10));
				for (Notification notification: iNotifications) {
					height += notification.getElement().getClientHeight() + 10;
					notification.getElement().getStyle().setProperty("left", (Window.getScrollLeft() + Window.getClientWidth() - 445) + "px");
					notification.getElement().getStyle().setProperty("top", (Window.getScrollTop() + Window.getClientHeight() - height) + "px");
				}
			}
		}
		
		public static class Notification extends HTML {
			Notification(String text, String style) {
				super(text);
				setStyleName("unitime-Notification");
				addStyleName(style);
			}
			
			public String toString() { return getHTML(); }
			public int hashCode() { return getHTML().hashCode(); }
			public boolean equals(Object o) {
				if (o == null || !(o instanceof Notification)) return false;
				return ((Notification)o).getHTML().equals(getHTML());
			}
			
			public void hide() {
				RootPanel.get().remove(this);
			}
			
			public void show() {
				RootPanel.get().add(this, Window.getScrollLeft() + Window.getClientWidth() - 445, Window.getScrollTop() + Window.getClientHeight());
			}
		}
	}
}
