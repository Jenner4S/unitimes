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

create table student_note (
	uniqueid decimal(20,0) primary key not null,
	student_id decimal(20,0) not null,
	text_note varchar(1000),
	time_stamp datetime not null,
	user_id varchar(40)
) engine = INNODB;

alter table student_note
	add constraint fk_student_note_student foreign key (student_id)
	references student (uniqueid) on delete cascade;

/*
 * Update database version
 */

update application_config set value='170' where name='tmtbl.db.version';

commit;
