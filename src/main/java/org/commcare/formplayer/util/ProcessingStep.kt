package org.commcare.formplayer.util

import org.apache.commons.logging.LogFactory
import org.commcare.formplayer.beans.SubmitResponseBean
import org.commcare.formplayer.objects.SerializableFormSession.SubmitStatus
import org.commcare.formplayer.services.FormSessionService
import org.commcare.formplayer.utils.CheckedFunction

class ProcessingStep(
    private val name: String,
    private val step: CheckedFunction<FormSubmissionContext, SubmitResponseBean, Exception>,
    private val context: FormSubmissionContext,
    private val formSessionService: FormSessionService,
    private val checkpoint: SubmitStatus?
) {

    private val log = LogFactory.getLog(ProcessingStep::class.java)

    @Throws(Exception::class)
    fun execute(): SubmitResponseBean {
        val session = context.formEntrySession.serializableSession
        if (checkpoint?.let { session.isProcessingStageComplete(it) } == true) {
            // skip this step if it has already been run
            log.debug("Form submission step '$name': skipping")
            return context.success();
        }

        log.debug("Form submission step '$name': executing")
        return step.apply(context)
    }

    fun recordCheckpoint() {
        val session = context.formEntrySession.serializableSession
        checkpoint?.let {
            session.submitStatus = it
            formSessionService.saveSession(session)
        }
    }

    override fun toString(): String {
        return "ProcessingStep($name)";
    }

    class StepFactory(val context: FormSubmissionContext, val formSessionService: FormSessionService) {

        @JvmOverloads
        fun makeStep(name: String, step: CheckedFunction<FormSubmissionContext, SubmitResponseBean, Exception>, checkpoint: SubmitStatus? = null): ProcessingStep {
            return ProcessingStep(name, step, context, formSessionService, checkpoint)
        }
    }
}
