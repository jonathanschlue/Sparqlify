package org.aksw.sparqlify.core.datatypes;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.aksw.commons.util.Pair;
import org.aksw.sparqlify.algebra.sql.exprs2.S_ColumnRef;
import org.aksw.sparqlify.algebra.sql.exprs2.S_Constant;
import org.aksw.sparqlify.algebra.sql.exprs2.S_Equals;
import org.aksw.sparqlify.algebra.sql.exprs2.S_Serialize;
import org.aksw.sparqlify.algebra.sql.exprs2.SqlExpr;
import org.aksw.sparqlify.core.TypeToken;

class SqlFunctionSerializerOp2
	extends SqlFunctionSerializerBase2
{
	private String opSymbol;
	
	public SqlFunctionSerializerOp2(String opSymbol) {
		this.opSymbol = opSymbol;
	}


	@Override
	public String serialize(String a, String b) {
		String result = a + " " + opSymbol + " " + b;
		return result;
	}
}

abstract class SqlFunctionSerializerBase2
	implements SqlFunctionSerializer
{		
	@Override
	public String serialize(List<String> args) {
		if(args.size() != 2) {
			throw new RuntimeException("Exactly 2 arguments expected, got: " + args);
		}
		
		String result = serialize(args.get(0), args.get(1));
		return result;
	}
	
	public abstract String serialize(String a, String b);	
}




/**
 * 
 * 
 * 
 * @author Claus Stadler <cstadler@informatik.uni-leipzig.de>
 *
 */
public class SqlExprEvaluator_Equals
	extends SqlExprEvaluator2
{
	private DatatypeSystem datatypeSystem;
	private SqlFunctionSerializer serializer;
	
	public SqlExprEvaluator_Equals(DatatypeSystem datatypeSystem) {
		this.datatypeSystem = datatypeSystem;
		this.serializer = new SqlFunctionSerializerOp2("=");
	}
	
	@Override
	public SqlExpr eval(SqlExpr a, SqlExpr b) {
		//SqlExpr result = SqlExprOps.logicalAnd(a, b);
		//return result;
		if(a.equals(S_Constant.TYPE_ERROR) || b.equals(S_Constant.TYPE_ERROR)) {
			return S_Constant.TYPE_ERROR;
		}

		if(getCommonDataype(a, b, datatypeSystem) == null) {
			
			
			Pair<? extends SqlExpr, ? extends SqlExpr> pair = resolveCast(a, b, datatypeSystem);
			if(pair == null) {
				return S_Constant.TYPE_ERROR;
			}
			
			if(getCommonDataype(pair.getKey(), pair.getValue(), datatypeSystem) == null) {
				return S_Constant.TYPE_ERROR;
			} else {
				SqlExpr result = new S_Serialize(TypeToken.Boolean, "=", Arrays.asList(pair.getKey(), pair.getValue()), serializer);
				//return new S_Equals(pair.getKey(), pair.getValue());
				return result;
			}
			
		}
		
		
		SqlExpr result = new S_Serialize(TypeToken.Boolean, "=", Arrays.asList(a, b), serializer);
		return result;

		
		
//		SqlExpr result;
//		
//		System.err.println("TODO: Check datatypes properly");
//		if(a.getDatatype().equals(b.getDatatype())) {
//			//Method
//			//XMethodImpl x = new XMethodImpl("=", null, null, serializer);
//
//			//SqlExpr result = new S_Method(x, Arrays.asList(a, b));
//			//return result;
//			result = new S_Serialize(TypeToken.Boolean, "=", Arrays.asList(a, b), serializer);
//		} else {
//			result = S_Constant.TYPE_ERROR;
//		}
//		
//		return result;
		
		// TODO Rule out incompatible datatype combinations
		//return S_Equals.create(a, b);
	}
	
	
	
	public static TypeToken getCommonDataype(SqlExpr left, SqlExpr right, DatatypeSystem system) {
		Set<TypeToken> commons = system.supremumDatatypes(left.getDatatype(), right.getDatatype());

		// TODO We should probably return type error here
		if(commons.isEmpty()) {
			return null;
		}

		if(commons.size() > 1) {
			throw new RuntimeException("Ambiguous type candidates: " + commons);
		}
		
		return commons.iterator().next();
	}
	
	public static S_Constant asConstant(SqlExpr expr) {
		return (expr instanceof S_Constant)
				? (S_Constant)expr
				: null;
	}
	
	public static S_ColumnRef asColumn(SqlExpr expr) {
		return (expr instanceof S_ColumnRef)
				? (S_ColumnRef)expr
				: null;		
	}
	
	
//	public static S_Constant tryCast(S_Constant value, SqlDatatype datatype) {
//		return value;
//	}
	
	public static Pair<? extends SqlExpr, ? extends SqlExpr> resolveCast(SqlExpr left, SqlExpr right, DatatypeSystem system) {
		Pair<S_ColumnRef, S_Constant> pair = tryMatch(left, right);
		if(pair == null) {
			return Pair.create(left, right);
		}

		try {
		
		if(pair.getKey().getDatatype().equals(pair.getValue().getDatatype())) {
			return pair;
		}
		
		} catch(Throwable t) {
			System.out.println("ffs");
		}
		
		
		Object value = pair.getValue().getValue();

		TypeToken targetType = pair.getKey().getDatatype();
		Object castedValue = system.cast(value, targetType);
		if(castedValue == null) {
			return null;
		}
		
		
		return Pair.create(pair.getKey(), new S_Constant(targetType, castedValue));
		
	}
	
	public static Pair<S_ColumnRef, S_Constant> tryMatch(SqlExpr left, SqlExpr right) {
		Pair<S_ColumnRef, S_Constant> result = tryMatchDirected(left, right);
		if(result == null) {
			result = tryMatchDirected(right, left);
		}
		
		return result;
	}
	
	public static Pair<S_ColumnRef, S_Constant> tryMatchDirected(SqlExpr left, SqlExpr right) {
		S_ColumnRef column = asColumn(left);
		if(column == null) {
			return null;
		}
		
		S_Constant value = asConstant(right);
		if(value == null) {
			return null;
		}
		
		return Pair.create(column, value);
	}
	
	
	
//	public static SqlExpr create(SqlExpr left, SqlExpr right, DatatypeSystem system) {
//		
//		// TODO Should we allow conversions, such as in '?var::int = 123456::string?
//		
//		// If one var is a constant, and the other is a variable, and the types differ,
//		// try casting the constant
//
//		
//		
//		
//		
//		// TODO We should probably return type error here
//		if(getCommonDataype(left, right, system) == null) {
//			
//			
//			Pair<? extends SqlExpr, ? extends SqlExpr> pair = resolveCast(left, right, system);
//			if(pair == null) {
//				return S_Constant.FALSE;
//			}
//			
//			if(getCommonDataype(pair.getKey(), pair.getValue(), system) == null) {
//				return S_Constant.FALSE;
//			} else {
//				return new S_Equals(pair.getKey(), pair.getValue());
//			}
//			
//		}
//		
//		//SqlDatatype common = commons.iterator().next(); 
//		
//		/*
//		// TODO A quick hack for testing
//		if(left.getDatatype() instanceof SqlDatatypeGeography && right.getDatatype() instanceof SqlDatatypeString)
//			return SqlExprValue.FALSE;
//		
//		if(right.getDatatype() instanceof SqlDatatypeGeography && left.getDatatype() instanceof SqlDatatypeString)
//			return SqlExprValue.FALSE;
//		*/
//		
//		return new S_Equals(left, right);
//	}


	
}