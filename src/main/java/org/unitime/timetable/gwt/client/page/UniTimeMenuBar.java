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

import java.util.List;

import org.unitime.timetable.gwt.client.Client;
import org.unitime.timetable.gwt.client.Pages;
import org.unitime.timetable.gwt.client.ToolBox;
import org.unitime.timetable.gwt.client.Client.GwtPageChangeEvent;
import org.unitime.timetable.gwt.client.Client.GwtPageChangedHandler;
import org.unitime.timetable.gwt.client.widgets.LoadingWidget;
import org.unitime.timetable.gwt.client.widgets.UniTimeFrameDialog;
import org.unitime.timetable.gwt.command.client.GwtRpcResponseList;
import org.unitime.timetable.gwt.command.client.GwtRpcService;
import org.unitime.timetable.gwt.command.client.GwtRpcServiceAsync;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.shared.MenuInterface;
import org.unitime.timetable.gwt.shared.MenuInterface.PageNameInterface;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ScrollEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MenuItemSeparator;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 * @author Tomas Muller
 */
public class UniTimeMenuBar extends UniTimeMenu {
	protected static final GwtMessages MESSAGES = GWT.create(GwtMessages.class);
	protected static final GwtRpcServiceAsync RPC = GWT.create(GwtRpcService.class);
	
	private MenuBar iMenu;
	private SimplePanel iSimple = null;
	
	private int iLastScrollLeft = 0, iLastScrollTop = 0, iLastClientWidth = 0;
	private Timer iMoveTimer;
	private HandlerRegistration iPageLabelRegistration = null;
	
	public UniTimeMenuBar(boolean absolute) {
		iMenu = new MenuBar();
		iMenu.addStyleName("unitime-NoPrint");
		iMenu.addStyleName("unitime-Menu");
		initWidget(iMenu);
		
		if (absolute) {
			iMenu.getElement().getStyle().setPosition(Position.ABSOLUTE);
			move(false);
			iMoveTimer = new Timer() {
				@Override
				public void run() {
					move(true);
				}
			};
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
			iSimple = new SimplePanel();
			iSimple.getElement().getStyle().setHeight(23, Unit.PX);
			new Timer() {
				@Override
				public void run() {
					delayedMove();
				}
			}.scheduleRepeating(5000);
		}
		
	}
	
	private void attach(final RootPanel rootPanel) {
		RPC.execute(new MenuInterface.MenuRpcRequest(), new AsyncCallback<GwtRpcResponseList<MenuInterface>>() {
			@Override
			public void onSuccess(GwtRpcResponseList<MenuInterface> result) {
				initMenu(iMenu, result, 0);
				if (iSimple != null)
					rootPanel.add(iSimple);
				rootPanel.add(UniTimeMenuBar.this);
				if (iSimple != null)
					iSimple.setHeight(iMenu.getOffsetHeight() + "px");
			}
			@Override
			public void onFailure(Throwable caught) {
			}
		});
	}
	
	@Override
	public void reload() {
		RPC.execute(new MenuInterface.MenuRpcRequest(), new AsyncCallback<GwtRpcResponseList<MenuInterface>>() {
			@Override
			public void onSuccess(GwtRpcResponseList<MenuInterface> result) {
				iMenu.clearItems();
				if (iPageLabelRegistration != null) {
					iPageLabelRegistration.removeHandler();
					iPageLabelRegistration = null;
				}
				initMenu(iMenu, result, 0);
			}
			@Override
			public void onFailure(Throwable caught) {
			}
		});
	}
	
	private void move(boolean show) {
		iLastClientWidth = Window.getClientWidth();
		iLastScrollLeft = Window.getScrollLeft();
		iLastScrollTop = Window.getScrollTop();
		iMenu.getElement().getStyle().setWidth(iLastClientWidth - 2, Unit.PX);
		iMenu.getElement().getStyle().setLeft(iLastScrollLeft, Unit.PX);
		iMenu.getElement().getStyle().setTop(iLastScrollTop, Unit.PX);
		iMenu.setVisible(true);
	}
	
	private boolean needsMove() {
		return iLastClientWidth != Window.getClientWidth() ||
			iLastScrollLeft != Window.getScrollLeft() || iLastScrollTop != Window.getScrollTop();
	}
	
	private void delayedMove() {
		if (needsMove()) {
			iMenu.setVisible(false);
			iMoveTimer.schedule(100);
		}
	}
	
	private void initMenu(MenuBar menu, List<MenuInterface> items, int level) {
		final MenuInterface.ValueEncoder encoder = new MenuInterface.ValueEncoder() {
			@Override
			public String encode(String value) {
				return URL.encodeQueryString(value);
			}
		};
		MenuItemSeparator lastSeparator = null;
		for (final MenuInterface item: items) {
			if (item.isSeparator()) {
				lastSeparator = new MenuItemSeparator();
				menu.addSeparator(lastSeparator);
			} else if (item.hasSubMenus()) {
				if (!item.hasPage()) {
					MenuBar m = new MenuBar(true);
					initMenu(m, item.getSubMenus(), level + 1);
					menu.addItem(new MenuItem(item.getName().replace(" ", "&nbsp;"), true, m));
				} else if ("PAGE_HELP".equals(item.getPage())) {
					PageNameInterface name = UniTimePageLabel.getInstance().getValue();
					final MenuItem m = new MenuItem(item.getName().replace(" ", "&nbsp;"), true, new Command() {
						@Override
						public void execute() {
							PageNameInterface name = UniTimePageLabel.getInstance().getValue();
							if (name.hasHelpUrl())
								openUrl(MESSAGES.pageHelp(name.getName()), name.getHelpUrl(), item.getTarget());
						}
					});
					m.setEnabled(name.hasHelpUrl());
					iPageLabelRegistration = UniTimePageLabel.getInstance().addValueChangeHandler(new ValueChangeHandler<MenuInterface.PageNameInterface>() {
						@Override
						public void onValueChange(ValueChangeEvent<PageNameInterface> event) {
							m.setEnabled(event.getValue().hasHelpUrl());
						}
					});
					menu.addItem(m);
					initMenu(menu, item.getSubMenus(), level);
				} else {
					menu.addItem(new MenuItem(item.getName().replace(" ", "&nbsp;"), true, new Command() {
						@Override
						public void execute() {
							if (item.hasPage())
								openUrl(item.getName(), item.getURL(encoder), item.getTarget());
						}
					}));
					initMenu(menu, item.getSubMenus(), level);
				}
			} else if ("PAGE_HELP".equals(item.getPage())) {
				PageNameInterface name = UniTimePageLabel.getInstance().getValue();
				final MenuItem m = new MenuItem(item.getName().replace(" ", "&nbsp;"), true, new Command() {
					@Override
					public void execute() {
						PageNameInterface name = UniTimePageLabel.getInstance().getValue();
						if (name.hasHelpUrl())
							openUrl(MESSAGES.pageHelp(name.getName()), name.getHelpUrl(), item.getTarget());
					}
				});
				m.setEnabled(name.hasHelpUrl());
				iPageLabelRegistration = UniTimePageLabel.getInstance().addValueChangeHandler(new ValueChangeHandler<MenuInterface.PageNameInterface>() {
					@Override
					public void onValueChange(ValueChangeEvent<PageNameInterface> event) {
						m.setEnabled(event.getValue().hasHelpUrl());
					}
				});
				menu.addItem(m);
			} else {
				menu.addItem(new MenuItem(item.getName().replace(" ", "&nbsp;"), true, new Command() {
					@Override
					public void execute() {
						if (item.hasPage())
							openUrl(item.getName(), item.getURL(encoder), item.getTarget());
					}
				}));
			}
		}
		if (level == 0 && lastSeparator != null) {
			lastSeparator.setStyleName("unitime-BlankSeparator");
			lastSeparator.getElement().getStyle().setWidth(100, Unit.PCT);
		}
	}
	
	protected void openUrl(String name, String url, String target) {
		if (target == null)
			LoadingWidget.getInstance().show();
		if ("dialog".equals(target)) {
			UniTimeFrameDialog.openDialog(name, url);
		} else if ("eval".equals(target)) {
			ToolBox.eval(url);
		} else if ("download".equals(target)) {
			ToolBox.open(url);
		} else {
			ToolBox.open(GWT.getHostPageBaseURL() + url);
		}
	}
	
	protected void openPage(String page) {
		try {
			for (Pages p: Pages.values()) {
				if (p.name().equals(page)) {
					LoadingWidget.getInstance().setMessage(MESSAGES.waitLoading(p.name(MESSAGES)));
					UniTimePageLabel.getInstance().setPageName(p.name(MESSAGES));
					RootPanel.get("UniTimeGWT:Body").add(p.widget());
					return;
				}
			}
			Label error = new Label(page == null ? MESSAGES.failedToLoadPageNotProvided() : MESSAGES.failedToLoadPageNotRegistered(page));
			error.setStyleName("unitime-ErrorMessage");
			RootPanel.get("UniTimeGWT:Body").add(error);
		} catch (Exception e) {
			Label error = new Label(MESSAGES.failedToLoadPage(e.getMessage()));
			error.setStyleName("unitime-ErrorMessage");
			RootPanel.get("UniTimeGWT:Body").add(error);
			UniTimeNotifications.error(MESSAGES.failedToLoadPage(e.getMessage()), e);
		}
	}
	
	public void insert(final RootPanel panel) {
		if ("hide".equals(Window.Location.getParameter("menu")))
			panel.setVisible(false);
		else
			attach(panel);
	}

}
