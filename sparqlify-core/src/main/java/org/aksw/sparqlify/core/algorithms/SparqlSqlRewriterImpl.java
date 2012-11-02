package org.aksw.sparqlify.core.algorithms;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.aksw.sparqlify.algebra.sql.nodes.SqlOp;
import org.aksw.sparqlify.core.domain.input.Mapping;
import org.aksw.sparqlify.core.domain.input.RestrictedExpr;
import org.aksw.sparqlify.core.domain.input.SparqlSqlRewrite;
import org.aksw.sparqlify.core.domain.input.VarDefinition;
import org.aksw.sparqlify.core.interfaces.CandidateViewSelector;
import org.aksw.sparqlify.core.interfaces.OpMappingRewriter;
import org.aksw.sparqlify.core.interfaces.SparqlSqlRewriter;
import org.aksw.sparqlify.core.interfaces.SqlOpSelectBlockCollector;
import org.aksw.sparqlify.core.interfaces.SqlOpSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import antlr.StringUtils;

import com.google.common.collect.Multimap;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.core.Var;

public class SparqlSqlRewriterImpl
	implements SparqlSqlRewriter
{
	private static final Logger logger = LoggerFactory.getLogger(SparqlSqlRewriterImpl.class);
	
	private CandidateViewSelector candidateViewSelector;
	private OpMappingRewriter opMappingRewriter;
	private SqlOpSelectBlockCollector sqlOpSelectBlockCollector;
	private SqlOpSerializer sqlOpSerializer;
	
	public SparqlSqlRewriterImpl(
			CandidateViewSelector candidateViewSelector,
			OpMappingRewriter opMappingRewriter,
			SqlOpSelectBlockCollector sqlOpSelectBlockCollector,
			SqlOpSerializer sqlOpSerializer)
	{
		this.candidateViewSelector = candidateViewSelector;
		this.opMappingRewriter = opMappingRewriter;
		this.sqlOpSelectBlockCollector = sqlOpSelectBlockCollector;
		this.sqlOpSerializer = sqlOpSerializer;
	}
	
	
	@Override
	public SparqlSqlRewrite rewrite(Query query) {
		/*
		if (!query.isSelectType()) {
			throw new RuntimeException("SELECT query expected. Got: ["
					+ query.toString() + "]");
		}
		*/
		
		Op opViewInstance = candidateViewSelector.getApplicableViews(query);

		logger.debug("View Instance Op: " + opViewInstance);
		

		// Get the projection order right in the result set
		List<Var> projectionOrder = null;
		if(query.isSelectType() && query.isQueryResultStar()) {
			projectionOrder = query.getProjectVars();
		} else {
			projectionOrder = query.getProjectVars();
		}

		
		//
		
		/*
		try {
			sqlNode = sqlRewriter.rewriteMM(view);
		} catch(EmptyRewriteException e) {
			return createEmptyResultSet();
		}*/

		/*
		if(logger.isInfoEnabled()) {
			logger.info("Final sparql var mapping:");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			write(ps, sqlNode.getSparqlVarToExprs());
			
			logger.info(baos.toString());
		}*/

		
		/*
		if(sqlNode instanceof SqlNodeEmpty) {
			return createEmptyResultSet();
		}*/

		Mapping mapping = opMappingRewriter.rewrite(opViewInstance);
		//logger.debug("Mapping:\n" + mapping);
		logger.debug("Variable Definitions:\n" + toIndentedString(mapping.getVarDefinition()));
		
		// FIXME Make the collector configurable
		//SqlOp block = SqlOpSelectBlockCollector._makeSelect(mapping.getSqlOp());
		SqlOp block = sqlOpSelectBlockCollector.transform(mapping.getSqlOp());

		
		String sqlQueryString = sqlOpSerializer.serialize(block);
		logger.info("Query String:\n" + sqlQueryString);


		SparqlSqlRewrite result = new SparqlSqlRewrite(sqlQueryString, mapping.getVarDefinition(), projectionOrder);
		
		return result;
	}
	
	public static String toIndentedString(VarDefinition varDef) {
		Multimap<Var, RestrictedExpr> map = varDef.getMap();
		String result = toIndentedString(map);
		return result;
	}
	
	public static String toIndentedString(Multimap<Var, RestrictedExpr> varToExprs) {
		
		String result = "";
		
		for(Entry<Var, Collection<RestrictedExpr>> entry : varToExprs.asMap().entrySet()) {
			Var var = entry.getKey();
			String varName = var.getName();
			int varLen = varName.length();
			Collection<RestrictedExpr> restExprs = entry.getValue();
			
			Iterator<RestrictedExpr> it = restExprs.iterator();
			
			String firstLabel;
			if(!it.hasNext()) {
				firstLabel = "(empty definition set)";
			} else {
				RestrictedExpr restExpr = it.next();
				firstLabel = varName + ": " + restExpr.getExpr() + " [" + restExpr.getRestrictions() + "]";
			}
			result += firstLabel + "\n";
			
			while(it.hasNext()) {
				RestrictedExpr restExpr = it.next();
				//StringUtils.
				// FIXME Make spaces for var length
				result += "    " + restExpr.getExpr() + " [" + restExpr.getRestrictions() + "]" + "\n";
			}
		}
		
		return result;
	}
}