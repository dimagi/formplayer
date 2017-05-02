package beans.menus;

import org.commcare.modern.util.Pair;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.commcare.util.screen.ScreenUtils;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.trace.AccumulatingReporter;
import org.javarosa.core.model.trace.EvaluationTraceReporter;
import org.javarosa.xpath.XPathException;

/**
 * Created by willpride on 5/2/17.
 */
public class EntityScreenUtils {

    private static final int SCREEN_WIDTH = 100;

    public static String[] getRows(TreeReference[] references,
                                   EvaluationContext evaluationContext,
                                   Detail detail) {
        String[] rows = new String[references.length];
        int i = 0;
        for (TreeReference entity : references) {
            rows[i] = createRow(entity, evaluationContext, detail);
            ++i;
        }
        return rows;
    }

    private static String createRow(TreeReference entity, EvaluationContext evaluationContext, Detail detail) {
        return createRow(entity, false, evaluationContext, detail);
    }

    private static String createRow(TreeReference entity,
                                    boolean collectDebug,
                                    EvaluationContext evaluationContext,
                                    Detail detail) {
        EvaluationContext context = new EvaluationContext(evaluationContext, entity);
        EvaluationTraceReporter reporter = new AccumulatingReporter();

        if (collectDebug) {
            context.setDebugModeOn(reporter);
        }
        detail.populateEvaluationContextVariables(context);

        if (collectDebug) {
            ScreenUtils.printAndClearTraces(reporter, "Variable Traces");
        }

        DetailField[] fields = detail.getFields();

        StringBuilder row = new StringBuilder();
        int i = 0;
        for (DetailField field : fields) {
            Object o;
            try {
                o = field.getTemplate().evaluate(context);
            } catch (XPathException e) {
                o = "error (see output)";
                e.printStackTrace();
            }
            String s;
            if (!(o instanceof String)) {
                s = "";
            } else {
                s = (String)o;
            }

            row.append(s);
        }

        if (collectDebug) {
            ScreenUtils.printAndClearTraces(reporter, "Template Traces:");
        }
        return row.toString();
    }

    public static Pair<String[], int[]> getHeaders(Detail shortDetail, EvaluationContext context){
        DetailField[] fields = shortDetail.getFields();
        String[] headers = new String[fields.length];
        int[] widthHints = new int[fields.length];

        StringBuilder row = new StringBuilder();
        int i = 0;
        for (DetailField field : fields) {
            String s = field.getHeader().evaluate(context);

            int widthHint = SCREEN_WIDTH / fields.length;
            try {
                widthHint = Integer.parseInt(field.getHeaderWidthHint());
            } catch (Exception e) {
                //Really don't care if it didn't work
            }
            ScreenUtils.addPaddedStringToBuilder(row, s, widthHint);

            headers[i] = s;
            widthHints[i] = widthHint;

            i++;
            if (i != fields.length) {
                row.append(" | ");
            }
        }
        return new Pair<>(headers, widthHints);
    }

    //So annoying how identical this is...
    private static String createHeader(Detail shortDetail, EvaluationContext context) {
        DetailField[] fields = shortDetail.getFields();

        StringBuilder row = new StringBuilder();
        int i = 0;
        for (DetailField field : fields) {
            String s = field.getHeader().evaluate(context);

            int widthHint = SCREEN_WIDTH / fields.length;
            try {
                widthHint = Integer.parseInt(field.getHeaderWidthHint());
            } catch (Exception e) {
                //Really don't care if it didn't work
            }
            ScreenUtils.addPaddedStringToBuilder(row, s, widthHint);
            i++;
            if (i != fields.length) {
                row.append(" | ");
            }
        }
        return row.toString();
    }
}
