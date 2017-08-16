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

alter table course_credit_unit_config add course_id decimal(20,0);
update course_credit_unit_config g set g.course_id = (select c.uniqueid from course_offering c where c.is_control = 1 and c.instr_offr_id = g.instr_offr_id) where g.instr_offr_id is not null;
alter table course_credit_unit_config add constraint fk_crs_crdt_unit_cfg_crs_own foreign key (course_id) references course_offering(uniqueid);
alter table course_credit_unit_config drop foreign key fk_crs_crdt_unit_cfg_io_own;
alter table course_credit_unit_config drop column instr_offr_id;

/*
 * Update database version
 */

update application_config set value='135' where name='tmtbl.db.version';

commit;
