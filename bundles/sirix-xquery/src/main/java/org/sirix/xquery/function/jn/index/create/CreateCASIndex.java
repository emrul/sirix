package org.sirix.xquery.function.jn.index.create;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.Namespaces;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.util.path.Path;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;
import org.brackit.xquery.xdm.Type;
import org.sirix.access.trx.node.json.JsonIndexController;
import org.sirix.api.json.JsonNodeReadOnlyTrx;
import org.sirix.api.json.JsonNodeTrx;
import org.sirix.api.json.JsonResourceManager;
import org.sirix.exception.SirixIOException;
import org.sirix.index.IndexDef;
import org.sirix.index.IndexDefs;
import org.sirix.index.IndexType;
import org.sirix.xquery.function.jn.JNFun;
import org.sirix.xquery.json.JsonDBItem;
import com.google.common.collect.ImmutableSet;

/**
 * <p>
 * Function for creating CAS indexes on stored documents, optionally restricted to a set of paths
 * and a content type. If successful, this function returns the document-node. Supported signatures
 * are:
 * </p>
 * <ul>
 * <li><code>jn:create-cas-index($doc as json-item(), $type as xs:string?, $paths as xs:string*) as json-item()</code></li>
 * <li><code>jn:create-cas-index($doc as json-item(), $type as xs:string?) as json-item()</code></li>
 * <li><code>jn:create-cas-index($doc as json-item()) as json-item()</code></li>
 * </ul>
 *
 * @author Johannes Lichtenberger
 *
 */
public final class CreateCASIndex extends AbstractFunction {

  /** CAS index function name. */
  public final static QNm CREATE_CAS_INDEX = new QNm(JNFun.JN_NSURI, JNFun.JN_PREFIX, "create-cas-index");

  /**
   * Constructor.
   *
   * @param name the name of the function
   * @param signature the signature of the function
   */
  public CreateCASIndex(QNm name, Signature signature) {
    super(name, signature, true);
  }

  @Override
  public Sequence execute(StaticContext sctx, QueryContext ctx, Sequence[] args) {
    if (args.length != 2 && args.length != 3) {
      throw new QueryException(new QNm("No valid arguments specified!"));
    }

    final JsonDBItem doc = (JsonDBItem) args[0];
    final JsonNodeReadOnlyTrx rtx = doc.getTrx();
    final JsonResourceManager manager = rtx.getResourceManager();

    final Optional<JsonNodeTrx> optionalWriteTrx = manager.getNodeWriteTrx();
    final JsonNodeTrx wtx = optionalWriteTrx.orElseGet(() -> manager.beginNodeTrx());

    if (rtx.getRevisionNumber() < manager.getMostRecentRevisionNumber()) {
      wtx.revertTo(rtx.getRevisionNumber());
    }

    final JsonIndexController controller = wtx.getResourceManager().getWtxIndexController(wtx.getRevisionNumber() - 1);

    if (controller == null) {
      throw new QueryException(new QNm("Document not found: " + ((Str) args[1]).stringValue()));
    }

    Type type = null;
    if (args.length > 1 && args[1] != null) {
      final QNm name = new QNm(Namespaces.XS_NSURI, ((Str) args[1]).stringValue());
      type = sctx.getTypes().resolveAtomicType(name);
    }

    final Set<Path<QNm>> paths = new HashSet<>();
    if (args.length == 3 && args[2] != null) {
      final Iter it = args[2].iterate();
      Item next = it.next();
      while (next != null) {
        paths.add(Path.parse(((Str) next).stringValue()));
        next = it.next();
      }
    }

    final IndexDef idxDef = IndexDefs.createCASIdxDef(false, Optional.ofNullable(type), paths,
        controller.getIndexes().getNrOfIndexDefsWithType(IndexType.CAS));
    try {
      controller.createIndexes(ImmutableSet.of(idxDef), wtx);
    } catch (final SirixIOException e) {
      throw new QueryException(new QNm("I/O exception: " + e.getMessage()), e);
    }

    return idxDef.materialize();
  }
}
