package beans;

import edu.nyu.cs.javagit.api.DotGit;
import edu.nyu.cs.javagit.api.JavaGitException;
import edu.nyu.cs.javagit.api.commands.GitLogResponse;

import java.io.IOException;

/**
 * Created by willpride on 8/8/16.
 */
public class GitStatusBean {

    private String commitMessage;
    private String commitSha;

    public GitStatusBean(DotGit gitRespositroy) throws IOException, JavaGitException {
        GitLogResponse.Commit c = gitRespositroy.getLog().get(0);
        setCommitMessage(c.getMessage());
        setCommitSha(c.getSha());
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }
}
