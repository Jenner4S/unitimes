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
package org.unitime.timetable.gwt.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Tomas Muller
 */
public class Query implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private Term iQuery = null;
	
	public Query(String query) {
		iQuery = parse(query == null ? "" : query.trim());
	}
	
	public Query(Term query) {
		iQuery = query;
	}
	
	public Term getQuery() { return iQuery; }
	
	public boolean match(TermMatcher m) {
		return iQuery.match(m);
	}
	
	public String toString() {
		return iQuery.toString();
	}
	
	public String toString(QueryFormatter f) {
		return iQuery.toString(f);
	}
	
	public boolean hasAttribute(String... attr) {
		for (String a: attr)
			if (iQuery.hasAttribute(a)) return true;
		return false;
	}
	
	private static List<String> split(String query, String... splits) {
		List<String> ret = new ArrayList<String>();
		int bracket = 0;
		boolean quot = false;
		int last = 0;
		boolean white = false;
		loop: for (int i = 0; i < query.length(); i++) {
			if (query.charAt(i) == '"') {
				quot = !quot;
				white = !quot;
				continue;
			}
			if (!quot && query.charAt(i) == '(') { bracket ++; white = false; continue; }
			if (!quot && query.charAt(i) == ')') { bracket --; white = true; continue; }
			if (quot || bracket > 0 || (!white && query.charAt(i) != ' ')) {
				white = (query.charAt(i) == ' ');
				continue;
			}
			white = (query.charAt(i) == ' ');
			String q = query.substring(i).toLowerCase();
			for (String split: splits) {
				if (split.isEmpty() || q.startsWith(split + " ") || q.startsWith(split + "\"") || q.startsWith(split + "(")) {
					String x = query.substring(last, i).trim();
					if (split.isEmpty() && x.endsWith(":")) continue;
					if (!x.isEmpty()) ret.add(x);
					last = i + split.length();
					if (!split.isEmpty())
						i += split.length() - 1;
					continue loop;
				}
			}
		}
		String x = query.substring(last).trim();
		if (!x.isEmpty()) ret.add(x);
		return ret;
	}

	private static Term parse(String query) {
		List<String> splits;
		splits = split(query, "and", "&&", "&");
		if (splits.size() > 1) {
			CompositeTerm t = new AndTerm();
			for (String q: splits)
				t.add(parse(q));
			return t;
		}
		splits = split(query, "or", "||", "|");
		if (splits.size() > 1) {
			CompositeTerm t = new OrTerm();
			for (String q: splits)
				t.add(parse(q));
			return t;
		}
		splits = split(query, "");
		if (splits.size() > 1) {
			CompositeTerm and = new AndTerm();
			boolean not = false;
			splits: for (String q: splits) {
				if (q.equalsIgnoreCase("not") || q.equals("!")) { not = true; continue; }
				if (q.startsWith("!(")) {
					q = q.substring(1); not = true;
				} else if (q.toLowerCase().startsWith("not(")) {
					q = q.substring(3); not = true;
				}
				if (not) {
					and.add(new NotTerm(parse(q)));
					not = false;
				} else {
					Term t = parse(q);
					if (t instanceof AtomTerm) {
						AtomTerm a = (AtomTerm)t;
						for (Term x: and.terms()) {
							if (x instanceof AtomTerm && ((AtomTerm)x).sameAttribute(a)) {
								and.remove(x);
								OrTerm or = new OrTerm();
								or.add(x); or.add(a);
								and.add(or);
								continue splits;
							} else if (x instanceof OrTerm && ((OrTerm)x).terms().get(0) instanceof AtomTerm && ((AtomTerm)((OrTerm)x).terms().get(0)).sameAttribute(a)) {
								((OrTerm)x).terms().add(a);
								continue splits;
							}
						}
					}
					and.add(t);
				}
			}
			return and;
		}
		if (query.startsWith("(") && query.endsWith(")")) return parse(query.substring(1, query.length() - 1).trim());
		if (query.startsWith("\"") && query.endsWith("\"") && query.length() >= 2) return new AtomTerm(null, query.substring(1, query.length() - 1).trim());
		int idx = query.indexOf(':');
		if (idx >= 0) {
			return new AtomTerm(query.substring(0, idx).trim().toLowerCase(), query.substring(idx + 1).trim());
		} else {
			return new AtomTerm(null, query);
		}
	}
	
	public static interface Term extends Serializable {
		public boolean match(TermMatcher m);
		public String toString(QueryFormatter f);
		public boolean hasAttribute(String attribute);
	}

	public static abstract class CompositeTerm implements Term {
		private static final long serialVersionUID = 1L;
		private List<Term> iTerms = new ArrayList<Term>();

		public CompositeTerm() {}
		
		public CompositeTerm(Term... terms) {
			for (Term t: terms) add(t);
		}
		
		public CompositeTerm(Collection<Term> terms) {
			for (Term t: terms) add(t);
		}
		
		public void add(Term t) { iTerms.add(t); }
		
		public void remove(Term t) { iTerms.remove(t); }
		
		protected List<Term> terms() { return iTerms; }
		
		public abstract String getOp();
		
		public boolean hasAttribute(String attribute) {
			for (Term t: terms())
				if (t.hasAttribute(attribute)) return true;
			return false;
		}
		
		public String toString() {
			String ret = "";
			for (Term t: terms()) {
				if (!ret.isEmpty()) ret += " " + getOp() + " ";
				ret += t;
			}
			return (terms().size() > 1 ? "(" + ret + ")" : ret);
		}
		
		public String toString(QueryFormatter f) {
			String ret = "";
			for (Term t: terms()) {
				if (!ret.isEmpty()) ret += " " + getOp() + " ";
				ret += t.toString(f);
			}
			return (terms().size() > 1 ? "(" + ret + ")" : ret);
		}
	}
	
	public static class OrTerm extends CompositeTerm {
		private static final long serialVersionUID = 1L;
		public OrTerm() { super(); }
		public OrTerm(Term... terms) { super(terms); }
		public OrTerm(Collection<Term> terms) { super(terms); }
		
		public String getOp() { return "OR"; }
		
		public boolean match(TermMatcher m) {
			if (terms().isEmpty()) return true;
			for (Term t: terms())
				if (t.match(m)) return true;
			return false;
		}

	}
	
	public static class AndTerm extends CompositeTerm {
		private static final long serialVersionUID = 1L;
		public AndTerm() { super(); }
		public AndTerm(Term... terms) { super(terms); }
		public AndTerm(Collection<Term> terms) { super(terms); }
		
		public String getOp() { return "AND"; }
		
		public boolean match(TermMatcher m) {
			for (Term t: terms())
				if (!t.match(m)) return false;
			return true;
		}
	}
	
	public static class NotTerm implements Term {
		private static final long serialVersionUID = 1L;
		private Term iTerm;
		
		public NotTerm(Term t) {
			iTerm = t;
		}
		
		public boolean match(TermMatcher m) {
			return !iTerm.match(m);
		}
		
		public boolean hasAttribute(String attribute) {
			return iTerm.hasAttribute(attribute);
		}
		
		public String toString() { return "NOT " + iTerm.toString(); }
		
		public String toString(QueryFormatter f) { return "NOT " + iTerm.toString(f); }
	}

	public static class AtomTerm implements Term {
		private static final long serialVersionUID = 1L;
		private String iAttr, iBody;
		
		public AtomTerm(String attr, String body) {
			if (body.startsWith("\"") && body.endsWith("\"") && body.length()>1)
				body = body.substring(1, body.length() - 1);
			iAttr = attr; iBody = body;
		}
		
		public boolean match(TermMatcher m) {
			return m.match(iAttr, iBody);
		}
		
		public boolean hasAttribute(String attribute) {
			return attribute != null && attribute.equals(iAttr);
		}
		
		public boolean sameAttribute(AtomTerm t) {
			return t != null && hasAttribute(t.iAttr);
		}
		
		public String toString() { return (iAttr == null ? "" : iAttr + ":") + (iBody.indexOf(' ') >= 0 ? "\"" + iBody + "\"" : iBody); }
		
		public String toString(QueryFormatter f) { return f.format(iAttr, iBody); }
	}
	
	public static interface TermMatcher {
		public boolean match(String attr, String term);
	}
	
	public static interface QueryFormatter {
		String format(String attr, String term);
	}
	
	public static void main(String[] args) {
		System.out.println(parse("(dept:1124 or dept:1125) and area:bio"));
		System.out.println(parse("a \"b c\" or ddd f \"x:x\" x: s !(band or org) (a)or(b)"));
		System.out.println(parse("! f (a)or(b) d !d not x s"));
		System.out.println(parse(""));
		System.out.println(split("(a \"b c\")  ddd f", ""));
		System.out.println(split("a \"b c\" OR not ddd f", "or"));
		System.out.println(split("a or((\"b c\" or dddor) f) q", "or"));
	}
	
	
}