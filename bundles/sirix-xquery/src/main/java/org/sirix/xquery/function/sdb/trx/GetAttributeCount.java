package org.sirix.xquery.function.sdb.trx;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;
import org.sirix.xquery.function.sdb.SDBFun;
import org.sirix.xquery.node.XmlDBNode;

/**
 * <p>
 * Function for getting the number of attributes of the current node. Supported signature is:
 * </p>
 * <ul>
 * <li><code>sdb:get-attribute-count($doc as xs:node) as xs:int</code></li>
 * </ul>
 *
 * @author Johannes Lichtenberger
 *
 */
public final class GetAttributeCount extends AbstractFunction {

  /** Get most recent revision function name. */
  public final static QNm GET_ATTRIBUTE_COUNT = new QNm(SDBFun.SDB_NSURI, SDBFun.SDB_PREFIX, "attribute-count");

  /**
   * Constructor.
   *
   * @param name the name of the function
   * @param signature the signature of the function
   */
  public GetAttributeCount(final QNm name, final Signature signature) {
    super(name, signature, true);
  }

  @Override
  public Sequence execute(final StaticContext sctx, final QueryContext ctx, final Sequence[] args) {
    final XmlDBNode doc = ((XmlDBNode) args[0]);

    return new Int32(doc.getTrx().getAttributeCount());
  }
}
