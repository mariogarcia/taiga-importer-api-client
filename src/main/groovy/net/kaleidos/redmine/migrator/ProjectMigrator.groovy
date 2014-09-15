package net.kaleidos.redmine.migrator

import net.kaleidos.taiga.TaigaClient
import net.kaleidos.redmine.RedmineClient

import net.kaleidos.domain.IssueStatus as TaigaIssueStatus
import net.kaleidos.domain.Project as TaigaProject
import net.kaleidos.domain.Membership as TaigaMembership

import com.taskadapter.redmineapi.bean.User as RedmineUser
import com.taskadapter.redmineapi.bean.Project as RedmineProject
import com.taskadapter.redmineapi.bean.Membership as RedmineMembership
import com.taskadapter.redmineapi.bean.IssueStatus as RedmineIssueStatus

class ProjectMigrator implements Migrator {

    static final String SEVERITY_NORMAL = 'Normal'

    final TaigaClient taigaClient
    final RedmineClient redmineClient

    ProjectMigrator(final RedmineClient redmineClient, final TaigaClient taigaClient) {
        this.redmineClient = redmineClient
        this.taigaClient = taigaClient
    }

    List<TaigaProject> migrateAllProjects() {
        return redmineClient.findAllProject().collect(this.&migrateProject)
    }

    TaigaProject migrateProject(final RedmineProject redmineProject) {
        return saveProject(buildProjectFromRedmineProject(redmineProject))
    }

    TaigaProject buildProjectFromRedmineProject(final RedmineProject redmineProject) {
        List<TaigaMembership> memberships =
            redmineClient
                .findAllMembershipByProjectIdentifier(redmineProject.identifier)
                .collect(this.&transformToTaigaMembership)

        return new TaigaProject(
            name: "${redmineProject.name} - [${redmineProject.identifier}]" ,
            description: redmineProject.with { description ?: name },
            roles: memberships.role.unique(),
            memberships: memberships,
            issueTypes: issueTypes,
            issueStatuses: issueStatuses,
            issuePriorities: issuePriorities,
            issueSeverities: issueSeverities
        )
    }

    TaigaMembership transformToTaigaMembership(final RedmineMembership redmineMembership) {
        RedmineUser user = redmineClient.findUserFullById(redmineMembership.user.id)

        return new TaigaMembership(
            email: user.mail,
            role: redmineMembership.roles.name.first()
        )
    }

    List<String> getIssueTypes() {
        return redmineClient.findAllTracker().collect(extractName)
    }

    List<TaigaIssueStatus> getIssueStatuses() {
        return redmineClient.findAllIssueStatus().collect(this.&taigaIssueStatusFromRedmineIssueStatus)
    }

    TaigaIssueStatus taigaIssueStatusFromRedmineIssueStatus(final RedmineIssueStatus status) {
        return new TaigaIssueStatus(name: status.name, isClosed: status.isClosed())
    }

    List<String> getIssuePriorities() {
        return redmineClient.findAllIssuePriority().collect(extractName)
    }

    List<String> getIssueSeverities() {
        return [SEVERITY_NORMAL]
    }

    Closure<String> extractName = { it.name }

    TaigaProject saveProject(final TaigaProject taigaProject) {
        return taigaClient.createProject(taigaProject)
    }

}
