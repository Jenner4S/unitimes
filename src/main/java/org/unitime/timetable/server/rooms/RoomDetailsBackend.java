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
package org.unitime.timetable.server.rooms;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.crypto.codec.Base64;
import org.unitime.localization.impl.Localization;
import org.unitime.localization.messages.CourseMessages;
import org.unitime.timetable.defaults.ApplicationProperty;
import org.unitime.timetable.defaults.CommonValues;
import org.unitime.timetable.defaults.UserProperty;
import org.unitime.timetable.events.RoomFilterBackend;
import org.unitime.timetable.gwt.command.server.GwtRpcImplements;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.server.Query;
import org.unitime.timetable.gwt.shared.EventInterface.FilterRpcResponse;
import org.unitime.timetable.gwt.shared.RoomInterface.AcademicSessionInterface;
import org.unitime.timetable.gwt.shared.RoomInterface.BuildingInterface;
import org.unitime.timetable.gwt.shared.RoomInterface.DepartmentInterface;
import org.unitime.timetable.gwt.shared.RoomInterface.ExamTypeInterface;
import org.unitime.timetable.gwt.shared.RoomInterface.FeatureInterface;
import org.unitime.timetable.gwt.shared.RoomInterface.FeatureTypeInterface;
import org.unitime.timetable.gwt.shared.RoomInterface.FutureRoomInterface;
import org.unitime.timetable.gwt.shared.RoomInterface.GroupInterface;
import org.unitime.timetable.gwt.shared.RoomInterface.PreferenceInterface;
import org.unitime.timetable.gwt.shared.RoomInterface.RoomDetailInterface;
import org.unitime.timetable.gwt.shared.RoomInterface.RoomFilterRpcRequest;
import org.unitime.timetable.gwt.shared.RoomInterface.RoomPictureInterface;
import org.unitime.timetable.gwt.shared.RoomInterface.RoomTypeInterface;
import org.unitime.timetable.model.Building;
import org.unitime.timetable.model.ChangeLog;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.DepartmentRoomFeature;
import org.unitime.timetable.model.DepartmentStatusType;
import org.unitime.timetable.model.ExamType;
import org.unitime.timetable.model.Location;
import org.unitime.timetable.model.LocationPicture;
import org.unitime.timetable.model.PreferenceLevel;
import org.unitime.timetable.model.Room;
import org.unitime.timetable.model.RoomDept;
import org.unitime.timetable.model.RoomFeature;
import org.unitime.timetable.model.RoomFeatureType;
import org.unitime.timetable.model.RoomGroup;
import org.unitime.timetable.model.RoomTypeOption;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.UserAuthority;
import org.unitime.timetable.security.qualifiers.SimpleQualifier;
import org.unitime.timetable.security.rights.Right;

/**
 * @author Tomas Muller
 */
@GwtRpcImplements(RoomFilterRpcRequest.class)
public class RoomDetailsBackend extends RoomFilterBackend {
	protected static final GwtMessages MESSAGES = Localization.create(GwtMessages.class);
	public static final CourseMessages MSG = Localization.create(CourseMessages.class);
	
	@Override
	public FilterRpcResponse execute(org.unitime.timetable.gwt.shared.EventInterface.RoomFilterRpcRequest request, EventContext context) {
		return super.execute(request, context);
	}
	
	@Override
	protected void checkPermission(org.unitime.timetable.gwt.shared.EventInterface.RoomFilterRpcRequest request, SessionContext context) {
		context.checkPermissionAnyAuthority(Right.Rooms, new SimpleQualifier("Session", request.getSessionId()));
	}

	@Override
	protected boolean checkEventStatus() { return false; }
	
	@Override
	protected boolean showRoomFeature(RoomFeatureType type) { return true; }
	
	@Override
	public RoomQuery getQuery(Long sessionId, Map<String, Set<String>> options, EventContext context) {
		RoomQuery query = super.getQuery(sessionId, options, context);
		
		Set<String> flags = (options == null ? null : options.get("flag"));
		boolean fetch = (flags != null && flags.contains("fetch"));
		if (fetch) {
			query.addFrom("fetch", "left join fetch l.roomDepts Frd " +
				" left join fetch l.examTypes Fxt" +
				" left join fetch l.features Ff" +
				" left join fetch l.roomGroups Fg" +
				" left join fetch l.roomType Ft");
		}
		
		return query;
	}
	
	@Override
	public void enumarate(org.unitime.timetable.gwt.shared.EventInterface.RoomFilterRpcRequest request, FilterRpcResponse response, EventContext context) {
		fixRoomFeatureTypes(request);
		request.addOption("flag", "fetch");

		String department = request.getOption("department");
    	boolean filterDepartments = !context.getUser().getCurrentAuthority().hasRight(Right.DepartmentIndependent);
    	List<ExamType> types = ExamType.findAllApplicable(context.getUser(), DepartmentStatusType.Status.ExamView, DepartmentStatusType.Status.ExamTimetable);
    	boolean html = true;
    	if (request.hasOptions("flag") && request.getOptions("flag").contains("plain")) {
    		html = false;
    	}
    	
    	UserAuthority authority = (context.getUser() == null ? null : context.getUser().getCurrentAuthority());
    	boolean courses = context.hasPermission(Right.InstructionalOfferings) || context.hasPermission(Right.Classes) ||
    			(authority != null && (authority.hasRight(Right.RoomEditChangeRoomProperties) || authority.hasRight(Right.RoomEditChangeControll) || authority.hasRight(Right.RoomDetailAvailability) || authority.hasRight(Right.RoomEditAvailability)));
    	boolean exams = context.hasPermission(Right.Examinations) ||
    			(authority != null && (authority.hasRight(Right.RoomEditChangeExaminationStatus) || authority.hasRight(Right.RoomDetailPeriodPreferences)));
    	boolean events = context.hasPermission(Right.Events) ||
    			(authority != null && (authority.hasRight(Right.RoomEditChangeEventProperties) || authority.hasRight(Right.RoomDetailEventAvailability ) || authority.hasRight(Right.RoomEditEventAvailability)));
    	boolean editPermissions = request.hasOption("id");

		Map<Long, Double> distances = new HashMap<Long, Double>();
		for (Location location: locations(request.getSessionId(), request.getOptions(), new Query(request.getText()), -1, distances, null, context)) {
			Double dist = distances.get(location.getUniqueId());
			RoomDetailInterface e = load(location, department, html, context, filterDepartments, types, courses, exams, events, editPermissions);
			if (editPermissions) {
				RoomSharingBackend rsb = new RoomSharingBackend();
				if (e.isCanSeeEventAvailability())
					e.setEventAvailabilityModel(rsb.loadEventAvailability(location, context));
				if (e.isCanSeeAvailability())
					e.setRoomSharingModel(rsb.loadRoomSharing(location, true, context));
				if (e.isCanSeePeriodPreferences()) {
					PeriodPreferencesBackend ppb = new PeriodPreferencesBackend();
					for (ExamType type: types) {
						e.setPeriodPreferenceModel(ppb.loadPeriodPreferences(location, type, context));
					}
				}
			}
			e.setProperty("permId", location.getPermanentId().toString());
			if (dist != null)
				e.setProperty("distance", String.valueOf(dist == null ? 0l : Math.round(dist)));
			e.setProperty("overbook", context.hasPermission(location, Right.EventLocationOverbook) ? "1" : "0");
			response.addResult(e);
		}
	}
	
	protected static DepartmentInterface wrap(Department d, Location location, PreferenceLevel pref) {
		if (d == null) return null;
		DepartmentInterface department = new DepartmentInterface();
		department.setId(d.getUniqueId());
		department.setDeptCode(d.getDeptCode());
		department.setAbbreviation(d.getAbbreviation());
		department.setLabel(d.getName());
		department.setExternal(d.isExternalManager());
		department.setEvent(d.isAllowEvents());
		department.setExtAbbreviation(d.getExternalMgrAbbv());
		department.setExtLabel(d.getExternalMgrLabel());
		department.setTitle(d.getLabel());
		if (pref != null && !PreferenceLevel.sNeutral.equals(pref.getPrefProlog()))
			department.setPreference(new PreferenceInterface(pref.getUniqueId(), PreferenceLevel.prolog2color(pref.getPrefProlog()), pref.getPrefProlog(), pref.getPrefName(), pref.getAbbreviation(), false));
		if (location != null)
			department.setColor("#" + d.getRoomSharingColor(null));
		return department;
	}
	
	protected RoomDetailInterface load(Location location, String department, boolean html, SessionContext context, boolean filterDepartments, List<ExamType> types, boolean courses, boolean exams, boolean events, boolean editPermissions) {
		RoomDetailInterface response = new RoomDetailInterface(location.getUniqueId(), location.getDisplayName(), location.getLabel());
		
		response.setCanShowDetail(context.hasPermission(location, Right.RoomDetail));
		response.setCanSeeAvailability(context.hasPermission(location, Right.RoomDetailAvailability));
		response.setCanSeeEventAvailability(context.hasPermission(location, Right.RoomDetailEventAvailability));
		response.setCanSeePeriodPreferences(context.hasPermission(location, Right.RoomDetailPeriodPreferences));
		if (location instanceof Room) {
			response.setCanChange(context.hasPermission(location, Right.RoomEdit));
		} else {
			response.setCanChange(context.hasPermission(location, Right.NonUniversityLocationEdit));
		}
		response.setCanChangeFeatures(context.hasPermission(location, Right.RoomEditFeatures) || context.hasPermission(location, Right.RoomEditGlobalFeatures));
		response.setCanChangeGroups(context.hasPermission(location, Right.RoomEditGroups) || context.hasPermission(location, Right.RoomEditGlobalGroups));
		response.setCanChangeEventAvailability(context.hasPermission(location, Right.RoomEditEventAvailability));
		response.setCanChangeAvailability(context.hasPermission(location, Right.RoomEditAvailability));
		response.setCanChangePicture(context.hasPermission(location, Right.RoomEditChangePicture));
		response.setCanChangePreferences(context.hasPermission(location, Right.RoomEditPreference));
		if (editPermissions) {
			response.setCanChangeEventProperties(context.hasPermission(location, Right.RoomEditChangeEventProperties));
			if (response.isCanChange()) {
				response.setCanChangeCapacity(context.hasPermission(location, Right.RoomEditChangeCapacity));
				response.setCanChangeControll(context.hasPermission(location, Right.RoomEditChangeControll));
				response.setCanChangeExamStatus(context.hasPermission(location, Right.RoomEditChangeExaminationStatus));
				response.setCanChangeExternalId(context.hasPermission(location, Right.RoomEditChangeExternalId));
				response.setCanChangeRoomProperties(context.hasPermission(location, Right.RoomEditChangeRoomProperties));
				response.setCanChangeType(context.hasPermission(location, Right.RoomEditChangeType));
			} else if (response.isCanChangeAvailability()) {
				response.setCanChangeControll(context.hasPermission(location, Right.RoomEditChangeControll));
			}
			if (location instanceof Room) {
				response.setCanDelete(context.hasPermission(location, Right.RoomDelete));
			} else {
				response.setCanDelete(context.hasPermission(location, Right.NonUniversityLocationDelete));
			}
			
			for (Location loc: location.getFutureLocations()) {
				FutureRoomInterface f = new FutureRoomInterface(loc.getUniqueId(), loc.getLabel());
				f.setSession(new AcademicSessionInterface(loc.getSession().getUniqueId(), loc.getSession().getAcademicTerm() + " " + loc.getSession().getAcademicYear()));
				f.setDisplayName(loc.getDisplayName());
				f.setExternalId(loc.getExternalUniqueId());
				f.setCapacity(loc.getCapacity());
				f.setType(loc.getRoomTypeLabel());
				EventContext cx = new EventContext(context, context.getUser(), loc.getSession().getUniqueId());
				if (loc instanceof Room) {
					f.setCanDelete(cx.hasPermission(loc, Right.RoomDelete));
					f.setCanChange(cx.hasPermission(loc, Right.RoomEdit));
				} else {
					f.setCanDelete(cx.hasPermission(loc, Right.NonUniversityLocationDelete));
					f.setCanChange(cx.hasPermission(loc, Right.NonUniversityLocationEdit));
				}
				if (f.isCanChange() || f.isCanDelete())
					response.addFutureRoom(f);
			}
		}
		
		if (location instanceof Room) {
			Room room = (Room)location;
			Building b = room.getBuilding(); 
			BuildingInterface building = new BuildingInterface(b.getUniqueId(), b.getAbbreviation(), b.getName());
			building.setX(b.getCoordinateX()); building.setY(b.getCoordinateY());
			building.setExternalId(b.getExternalUniqueId());
			response.setBuilding(building);
			response.setName(room.getRoomNumber());
		}
		
		response.setSessionId(location.getSession().getUniqueId());
		response.setSessionName(location.getSession().getLabel());
		response.setExternalId(location.getExternalUniqueId());
		response.setRoomType(new RoomTypeInterface(location.getRoomType().getUniqueId(), location.getRoomType().getReference(), location.getRoomType().getLabel(), location.getRoomType().isRoom(), location.getRoomType().getOrd()));
		response.setX(location.getCoordinateX());
		response.setY(location.getCoordinateY());
		response.setArea(location.getArea());;
    	response.setCapacity(location.getCapacity());
    	response.setExamCapacity(location.getExamCapacity());
    	
    	for (RoomFeature f: location.getFeatures()) {
    		FeatureInterface feature = new FeatureInterface(f.getUniqueId(), f.getAbbv(), f.getLabel());
    		feature.setDescription(f.getDescription());
    		if (f.getFeatureType() != null)
    			feature.setType(new FeatureTypeInterface(f.getFeatureType().getUniqueId(), f.getFeatureType().getReference(), f.getFeatureType().getLabel(), f.getFeatureType().isShowInEventManagement()));
    		if (f instanceof DepartmentRoomFeature) {
    			if (!courses) continue;
    			Department d = ((DepartmentRoomFeature)f).getDepartment();
    			if (filterDepartments && !context.getUser().getCurrentAuthority().hasQualifier(d)) continue; 
    			feature.setDepartment(wrap(d, location, null));
    			feature.setTitle((f.getDescription() == null || f.getDescription().isEmpty() ? f.getLabel() : f.getDescription()) + " (" + d.getName() + (f.getFeatureType() == null ? "" : ", " + f.getFeatureType().getLabel()) + ")");
    		} else {
    			feature.setTitle((f.getDescription() == null || f.getDescription().isEmpty() ? f.getLabel() : f.getDescription()) + (f.getFeatureType() == null ? "" : " (" + f.getFeatureType().getLabel() + ")"));
    		}
    		response.addFeature(feature);
    	}
    	for (RoomGroup g: location.getRoomGroups()) {
    		GroupInterface group = new GroupInterface(g.getUniqueId(), g.getAbbv(), g.getName());
    		if (g.getDepartment() != null) {
    			if (!courses) continue;
    			if (filterDepartments && !context.getUser().getCurrentAuthority().hasQualifier(g.getDepartment())) continue;
    			group.setDepartment(wrap(g.getDepartment(), location, null));
    			group.setTitle((g.getDescription() == null || g.getDescription().isEmpty() ? g.getName() : g.getDescription()) + " (" + g.getDepartment().getName() + ")");
    		}
    		response.addGroup(group);
    	}
    	
    	if (courses) {
        	response.setIgnoreRoomCheck(location.isIgnoreRoomCheck());
        	response.setIgnoreTooFar(location.isIgnoreTooFar());
        	
        	for (RoomDept rd: location.getRoomDepts()) {
        		DepartmentInterface d = wrap(rd.getDepartment(), location, rd.getPreference());
        		response.addDepartment(d);
        		if (rd.isControl())
        			response.setControlDepartment(d);
        	}
        	if (response.isCanSeeAvailability()) {
        		if (html)
        			response.setAvailability(location.getRoomSharingTable().getModel().toString().replaceAll(", ","<br>"));
        		else
        			response.setAvailability(location.getRoomSharingTable().getModel().toString().replaceAll(", ","\n"));
            	response.setRoomSharingNote(location.getShareNote());
        	}
    	}
    	if (events) {
    		if (response.isCanSeeEventAvailability()) {
    			if (html)
    				response.setEventAvailability(location.getEventAvailabilityTable().getModel().toString().replaceAll(", ", "<br>"));
    			else
    				response.setEventAvailability(location.getEventAvailabilityTable().getModel().toString().replaceAll(", ", "\n"));
    		}
    		
        	if (location.getEventDepartment() != null)
        		response.setEventDepartment(wrap(location.getEventDepartment(), location, null));
        	
        	response.setEventNote(location.getEventMessage());
        	response.setBreakTime(location.getBreakTime());
        	if (location.getEventDepartment() != null) {
            	response.setEventStatus(location.getEventStatus());
            	RoomTypeOption rto = location.getRoomType().getOption(location.getEventDepartment());
            	response.setDefaultEventStatus(rto.getStatus());
            	response.setDefaultBreakTime(rto.getBreakTime());
            	response.setDefaultEventNote(rto.getMessage());
        	} else {
        		response.setDefaultEventStatus(RoomTypeOption.Status.NoEventManagement.ordinal());
        	}
    	}
    	
    	if (exams) {
        	for (ExamType xt: location.getExamTypes()) {
        		if (types != null && !types.contains(xt)) continue;
        		if (xt.getReference().equals(department) && response.isCanSeePeriodPreferences()) {
        			if (html)
    					response.setPeriodPreference(location.getExamPreferencesAbbreviationHtml(xt));
    				else
    					response.setPeriodPreference(location.getExamPreferencesAbbreviation(xt));
        		}
        		response.addExamType(new ExamTypeInterface(xt.getUniqueId(), xt.getReference(), xt.getLabel(), xt.getType() == ExamType.sExamTypeFinal));
        	}
    	}
    	
		String minimap = ApplicationProperty.RoomHintMinimapUrl.value();
    	if (minimap != null && location.getCoordinateX() != null && location.getCoordinateY() != null)
    		response.setMiniMapUrl(minimap
    				.replace("%x", location.getCoordinateX().toString())
    				.replace("%y", location.getCoordinateY().toString())
    				.replace("%n", location.getLabel())
    				.replace("%i", location.getExternalUniqueId() == null ? "" : location.getExternalUniqueId()));
    	String map = ApplicationProperty.RoomMapStatic.value();
    	if (map != null && location.getCoordinateX() != null && location.getCoordinateY() != null)
    		response.setMapUrl(map
    				.replace("%x", location.getCoordinateX().toString())
    				.replace("%y", location.getCoordinateY().toString())
    				.replace("%n", location.getLabel())
    				.replace("%i", location.getExternalUniqueId() == null ? "" : location.getExternalUniqueId()));
    	String apikey = ApplicationProperty.RoomMapStaticApiKey.value();
    	String secret = ApplicationProperty.RoomMapStaticSecret.value();
    	if (response.hasMapUrl() && apikey != null && !apikey.isEmpty()) {
    		response.setMapUrl(response.getMapUrl() + "&key=" + apikey);
    		if (secret != null && !secret.isEmpty()) {
    			try {
					response.setMapUrl(response.getMapUrl() + "&signature=" + new UrlSigner(secret).signRequest(response.getMapUrl()));
				} catch (Exception e) {}
    		}
    	}
    	if (response.hasMiniMapUrl() && apikey != null && !apikey.isEmpty()) {
    		response.setMiniMapUrl(response.getMiniMapUrl() + "&key=" + apikey);
    		if (secret != null && !secret.isEmpty()) {
    			try {
					response.setMiniMapUrl(response.getMiniMapUrl() + "&signature=" + new UrlSigner(secret).signRequest(response.getMiniMapUrl()));
				} catch (Exception e) {}
    		}
    	}
    	
    	for (LocationPicture picture: new TreeSet<LocationPicture>(location.getPictures()))
    		response.addPicture(new RoomPictureInterface(picture.getUniqueId(), picture.getFileName(), picture.getContentType(), picture.getTimeStamp().getTime(), RoomPicturesBackend.getPictureType(picture.getType())));
    	
    	if (context.hasPermission(Right.HasRole) && CommonValues.Yes.eq(context.getUser().getProperty(UserProperty.DisplayLastChanges))) {
    		ChangeLog lch = ChangeLog.findLastChange(location.getClass().getName(), location.getUniqueId(), null);
            if (lch != null)
            	response.setLastChange(lch.getShortLabel());
    	}
    	
    	return response;
	}
	
	public static class UrlSigner {
		private static byte[] key;
		
		public UrlSigner(String keyString) throws IOException {
			keyString = keyString.replace('-', '+');
			keyString = keyString.replace('_', '/');
			key = Base64.decode(keyString.getBytes());
		}
		
		public String signRequest(String mapsUrl) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException, URISyntaxException, MalformedURLException {
			URL url = new URL(mapsUrl);
			String resource = url.getPath() + "?" + url.getQuery();
			SecretKeySpec sha1Key = new SecretKeySpec(key, "HmacSHA1");
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(sha1Key);
			byte[] sigBytes = mac.doFinal(resource.getBytes());
			String signature = new String(Base64.encode(sigBytes));
			signature = signature.replace('+', '-');
			signature = signature.replace('/', '_');
			return signature;
		}
	}
	
	public static void main(String[] args) throws Exception {
		String url = "https://maps.googleapis.com/maps/api/staticmap?key=AIzaSyDpUnTRgd4mB7HdVt5ug_LwKzdNfTQVA14&center=48.8584,2.2945&size=600x400&zoom=16&maptype=roadmap&sensor=false&markers=color:blue%7c48.8584,2.2945";
		System.out.println(url + "&signature=" + new UrlSigner("oJaEChTm5G0AZsqWVQJcb-9SXHY=").signRequest(url));
	}

}
