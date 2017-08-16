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

create table sectioning_queue (
	uniqueid number(20,0) constraint nn_sect_queue_uniqueid not null,
	session_id number(20,0) constraint nn_sect_queue_session not null,
	type number(10) constraint nn_sect_queue_type not null,
	time_stamp timestamp constraint nn_sect_queue_ts not null,
	message clob
);

alter table sectioning_queue add constraint pk_sect_queue primary key (uniqueid);

create index idx_sect_queue_session_ts on sectioning_queue(session_id, time_stamp);
		
/**
 * Update database version
 */

update application_config set value='59' where name='tmtbl.db.version';

commit;
		