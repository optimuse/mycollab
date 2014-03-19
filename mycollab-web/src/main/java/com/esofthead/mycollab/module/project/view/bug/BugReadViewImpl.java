/**
 * This file is part of mycollab-web.
 *
 * mycollab-web is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mycollab-web is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with mycollab-web.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.esofthead.mycollab.module.project.view.bug;

import java.util.List;

import org.vaadin.peter.buttongroup.ButtonGroup;

import com.esofthead.mycollab.common.CommentType;
import com.esofthead.mycollab.common.localization.GenericI18Enum;
import com.esofthead.mycollab.core.utils.LocalizationHelper;
import com.esofthead.mycollab.core.utils.StringUtils;
import com.esofthead.mycollab.eventmanager.EventBus;
import com.esofthead.mycollab.module.file.AttachmentType;
import com.esofthead.mycollab.module.project.CurrentProjectVariables;
import com.esofthead.mycollab.module.project.ProjectResources;
import com.esofthead.mycollab.module.project.ProjectRolePermissionCollections;
import com.esofthead.mycollab.module.project.events.BugComponentEvent;
import com.esofthead.mycollab.module.project.events.BugEvent;
import com.esofthead.mycollab.module.project.events.BugVersionEvent;
import com.esofthead.mycollab.module.project.events.MilestoneEvent;
import com.esofthead.mycollab.module.project.localization.TaskI18nEnum;
import com.esofthead.mycollab.module.project.ui.components.AbstractPreviewItemComp;
import com.esofthead.mycollab.module.project.ui.components.CommentDisplay;
import com.esofthead.mycollab.module.project.ui.components.DefaultProjectFormViewFieldFactory.ProjectFormAttachmentDisplayField;
import com.esofthead.mycollab.module.project.view.settings.component.ProjectUserFormLinkField;
import com.esofthead.mycollab.module.tracker.BugStatusConstants;
import com.esofthead.mycollab.module.tracker.domain.Component;
import com.esofthead.mycollab.module.tracker.domain.SimpleBug;
import com.esofthead.mycollab.module.tracker.domain.Version;
import com.esofthead.mycollab.module.tracker.service.BugService;
import com.esofthead.mycollab.schedule.email.project.BugRelayEmailNotificationAction;
import com.esofthead.mycollab.spring.ApplicationContextUtil;
import com.esofthead.mycollab.vaadin.AppContext;
import com.esofthead.mycollab.vaadin.events.HasPreviewFormHandlers;
import com.esofthead.mycollab.vaadin.mvp.ViewComponent;
import com.esofthead.mycollab.vaadin.ui.AbstractBeanFieldGroupViewFieldFactory;
import com.esofthead.mycollab.vaadin.ui.AdvancedPreviewBeanForm;
import com.esofthead.mycollab.vaadin.ui.DefaultFormViewFieldFactory;
import com.esofthead.mycollab.vaadin.ui.DefaultFormViewFieldFactory.FormContainerHorizontalViewField;
import com.esofthead.mycollab.vaadin.ui.DefaultFormViewFieldFactory.FormContainerViewField;
import com.esofthead.mycollab.vaadin.ui.DefaultFormViewFieldFactory.FormDateViewField;
import com.esofthead.mycollab.vaadin.ui.DefaultFormViewFieldFactory.FormLinkViewField;
import com.esofthead.mycollab.vaadin.ui.DefaultFormViewFieldFactory.FormViewField;
import com.esofthead.mycollab.vaadin.ui.GenericBeanForm;
import com.esofthead.mycollab.vaadin.ui.GridFormLayoutHelper;
import com.esofthead.mycollab.vaadin.ui.IFormLayoutFactory;
import com.esofthead.mycollab.vaadin.ui.MyCollabResource;
import com.esofthead.mycollab.vaadin.ui.ProjectPreviewFormControlsGenerator;
import com.esofthead.mycollab.vaadin.ui.UIConstants;
import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Field;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;

/**
 * 
 * @author MyCollab Ltd.
 * @since 1.0
 * 
 */
@ViewComponent
public class BugReadViewImpl extends AbstractPreviewItemComp<SimpleBug> implements BugReadView,
IBugCallbackStatusComp {

	private static final long serialVersionUID = 1L;

	private HorizontalLayout bugWorkflowControl;

	private BugHistoryList historyList;

	private BugFollowersSheet bugFollowersList;

	private BugTimeLogSheet bugTimeLogList;

	private BugRelatedField bugRelatedField;

	private CommentDisplay commentList;

	private ProjectPreviewFormControlsGenerator<SimpleBug> bugPreviewFormControls;

	public BugReadViewImpl() {
		super("Bug Detail", MyCollabResource.newResource("icons/24/project/bug.png"));
	}

	private void displayWorkflowControl() {
		if (BugStatusConstants.OPEN.equals(this.beanItem.getStatus())
				|| BugStatusConstants.REOPENNED.equals(this.beanItem.getStatus())) {
			this.bugWorkflowControl.removeAllComponents();
			final ButtonGroup navButton = new ButtonGroup();
			final Button startProgressBtn = new Button("Start Progress",
					new Button.ClickListener() {
				private static final long serialVersionUID = 1L;

				@Override
				public void buttonClick(final ClickEvent event) {
					beanItem
					.setStatus(BugStatusConstants.INPROGRESS);
					final BugService bugService = ApplicationContextUtil
							.getSpringBean(BugService.class);
					bugService.updateWithSession(
							beanItem,
							AppContext.getUsername());
					displayWorkflowControl();
				}
			});
			startProgressBtn.addStyleName(UIConstants.THEME_BROWN_LINK);
			navButton.addButton(startProgressBtn);

			final Button resolveBtn = new Button("Resolved",
					new Button.ClickListener() {
				private static final long serialVersionUID = 1L;

				@Override
				public void buttonClick(final ClickEvent event) {
					UI.getCurrent().addWindow(
							new ResolvedInputWindow(
									BugReadViewImpl.this,
									beanItem));
				}
			});
			resolveBtn.addStyleName(UIConstants.THEME_BROWN_LINK);
			navButton.addButton(resolveBtn);

			final Button wontFixBtn = new Button("Won't Fix",
					new Button.ClickListener() {
				private static final long serialVersionUID = 1L;

				@Override
				public void buttonClick(final ClickEvent event) {
					UI.getCurrent().addWindow(
							new WontFixExplainWindow(
									BugReadViewImpl.this,
									beanItem));
				}
			});
			wontFixBtn.addStyleName(UIConstants.THEME_BROWN_LINK);
			navButton.addButton(wontFixBtn);
			this.bugWorkflowControl.addComponent(navButton);
		} else if (BugStatusConstants.INPROGRESS.equals(this.beanItem.getStatus())) {
			this.bugWorkflowControl.removeAllComponents();
			final ButtonGroup navButton = new ButtonGroup();
			final Button stopProgressBtn = new Button("Stop Progress",
					new Button.ClickListener() {
				private static final long serialVersionUID = 1L;

				@Override
				public void buttonClick(final ClickEvent event) {
					beanItem
					.setStatus(BugStatusConstants.OPEN);
					final BugService bugService = ApplicationContextUtil
							.getSpringBean(BugService.class);
					bugService.updateWithSession(
							beanItem,
							AppContext.getUsername());
					displayWorkflowControl();
				}
			});
			stopProgressBtn.addStyleName(UIConstants.THEME_BROWN_LINK);
			navButton.addButton(stopProgressBtn);

			final Button resolveBtn = new Button("Resolved",
					new Button.ClickListener() {
				private static final long serialVersionUID = 1L;

				@Override
				public void buttonClick(final ClickEvent event) {
					UI.getCurrent().addWindow(
							new ResolvedInputWindow(
									BugReadViewImpl.this,
									beanItem));
				}
			});
			resolveBtn.addStyleName(UIConstants.THEME_BROWN_LINK);
			navButton.addButton(resolveBtn);
			this.bugWorkflowControl.addComponent(navButton);
		} else if (BugStatusConstants.VERIFIED.equals(this.beanItem.getStatus())) {
			this.bugWorkflowControl.removeAllComponents();
			final ButtonGroup navButton = new ButtonGroup();
			final Button reopenBtn = new Button("Reopen",
					new Button.ClickListener() {
				private static final long serialVersionUID = 1L;

				@Override
				public void buttonClick(final ClickEvent event) {
					UI.getCurrent().addWindow(
							new ReOpenWindow(BugReadViewImpl.this,
									beanItem));
				}
			});
			reopenBtn.addStyleName(UIConstants.THEME_BROWN_LINK);
			navButton.addButton(reopenBtn);

			this.bugWorkflowControl.addComponent(navButton);
		} else if (BugStatusConstants.RESOLVED.equals(this.beanItem.getStatus())) {
			this.bugWorkflowControl.removeAllComponents();
			final ButtonGroup navButton = new ButtonGroup();
			final Button reopenBtn = new Button("Reopen",
					new Button.ClickListener() {
				private static final long serialVersionUID = 1L;

				@Override
				public void buttonClick(final ClickEvent event) {
					UI.getCurrent().addWindow(
							new ReOpenWindow(BugReadViewImpl.this,
									beanItem));
				}
			});
			reopenBtn.addStyleName(UIConstants.THEME_BROWN_LINK);
			navButton.addButton(reopenBtn);

			final Button approveNCloseBtn = new Button("Approve & Close",
					new Button.ClickListener() {
				private static final long serialVersionUID = 1L;

				@Override
				public void buttonClick(final ClickEvent event) {
					UI.getCurrent().addWindow(
							new ApproveInputWindow(
									BugReadViewImpl.this,
									beanItem));
				}
			});
			approveNCloseBtn.addStyleName(UIConstants.THEME_BROWN_LINK);
			navButton.addButton(approveNCloseBtn);
			this.bugWorkflowControl.addComponent(navButton);
		} else if (BugStatusConstants.RESOLVED.equals(this.beanItem.getStatus())) {
			this.bugWorkflowControl.removeAllComponents();
			final ButtonGroup navButton = new ButtonGroup();
			final Button reopenBtn = new Button("Reopen",
					new Button.ClickListener() {
				private static final long serialVersionUID = 1L;

				@Override
				public void buttonClick(final ClickEvent event) {
					UI.getCurrent().addWindow(
							new ReOpenWindow(BugReadViewImpl.this,
									beanItem));
				}
			});
			reopenBtn.setStyleName(UIConstants.THEME_BROWN_LINK);
			navButton.addButton(reopenBtn);

			this.bugWorkflowControl.addComponent(navButton);
		}
		this.bugWorkflowControl.setEnabled(CurrentProjectVariables
				.canWrite(ProjectRolePermissionCollections.BUGS));
	}

	@Override
	public SimpleBug getItem() {
		return this.beanItem;
	}

	@Override
	public void previewItem(final SimpleBug item) {
		super.previewItem(item);
		displayWorkflowControl();
		this.previewLayout.clearTitleStyleName();
		if (BugStatusConstants.VERIFIED.equals(this.beanItem
				.getStatus())) {
			this.previewLayout.addTitleStyleName(UIConstants.LINK_COMPLETED);
		} else if (this.beanItem.isOverdue()) {
			this.previewLayout.addTitleStyleName(UIConstants.LINK_OVERDUE);
		}
	}

	@Override
	public void refreshBugItem() {
		EventBus.getInstance().fireEvent(
				new BugEvent.GotoRead(BugReadViewImpl.this, this.beanItem.getId()));
	}

	@Override
	protected void initRelatedComponents() {
		commentList = new CommentDisplay(CommentType.PRJ_BUG,
				CurrentProjectVariables.getProjectId(), true, true,
				BugRelayEmailNotificationAction.class);
		commentList.setMargin(true);

		historyList = new BugHistoryList(this.beanItem.getId());
		historyList.setMargin(true);

		bugFollowersList = new BugFollowersSheet(this.beanItem);

		bugTimeLogList = new BugTimeLogSheet(this.beanItem);
	}

	@Override
	protected void onPreviewItem() {
		commentList.loadComments(this.beanItem.getId());
		historyList.loadHistory(this.beanItem.getId());

		bugTimeLogList.setBean(this.beanItem);
		bugTimeLogList.loadTimeValue();

		bugFollowersList.displayMonitorItems();

		bugRelatedField = new BugRelatedField(this.beanItem);
	}

	@Override
	protected String initFormTitle() {
		return "[Issue " + this.beanItem.getBugkey()
				+ "#]: "
				+ this.beanItem.getSummary();
	}

	@Override
	protected AdvancedPreviewBeanForm<SimpleBug> initPreviewForm() {
		return new AdvancedPreviewBeanForm<SimpleBug>();
	}

	@Override
	protected IFormLayoutFactory initFormLayoutFactory() {
		return new FormLayoutFactory();
	}

	@Override
	protected AbstractBeanFieldGroupViewFieldFactory<SimpleBug> initBeanFormFieldFactory() {
		return new PreviewFormFieldFactory(this.previewForm);
	}

	@Override
	protected ComponentContainer createButtonControls() {
		bugPreviewFormControls = new ProjectPreviewFormControlsGenerator<SimpleBug>(previewForm);
		final HorizontalLayout topPanel = bugPreviewFormControls.createButtonControls(ProjectRolePermissionCollections.BUGS, false, false);

		final Button assignBtn = new Button("Assign",
				new Button.ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(final ClickEvent event) {
				UI.getCurrent().addWindow(
						new AssignBugWindow(
								BugReadViewImpl.this,
								beanItem));
			}
		});
		assignBtn.setEnabled(CurrentProjectVariables
				.canWrite(ProjectRolePermissionCollections.BUGS));
		assignBtn.setIcon(MyCollabResource
				.newResource("icons/16/assign.png"));

		assignBtn.setStyleName(UIConstants.THEME_GREEN_LINK);

		this.bugWorkflowControl = new HorizontalLayout();
		this.bugWorkflowControl.setMargin(false);
		this.bugWorkflowControl.addStyleName("workflow-controls");

		bugPreviewFormControls.insertToControlBlock(bugWorkflowControl);
		bugPreviewFormControls.insertToControlBlock(assignBtn);
		topPanel.setSizeUndefined();

		return topPanel;
	}

	@Override
	protected ComponentContainer createBottomPanel() {
		final TabSheet tabBugDetail = new TabSheet();
		tabBugDetail.setWidth("100%");

		tabBugDetail.addTab(commentList, "Comments", MyCollabResource
				.newResource("icons/16/project/gray/comment.png"));


		tabBugDetail.addTab(historyList, "History", MyCollabResource
				.newResource("icons/16/project/gray/history.png"));


		tabBugDetail.addTab(bugRelatedField, "Related Bugs",
				MyCollabResource
				.newResource("icons/16/project/gray/bug.png"));


		tabBugDetail
		.addTab(bugFollowersList,
				"Followers",
				MyCollabResource
				.newResource("icons/16/project/gray/follow.png"));


		tabBugDetail.addTab(bugTimeLogList, "Time", MyCollabResource
				.newResource("icons/16/project/gray/time.png"));

		return tabBugDetail;
	}

	private class FormLayoutFactory implements IFormLayoutFactory {
		private static final long serialVersionUID = 1L;
		private GridFormLayoutHelper informationLayout;

		@Override
		public boolean attachField(final Object propertyId,
				final Field<?> field) {
			if (propertyId.equals("summary")) {
				this.informationLayout.addComponent(field, "Summary", 0, 0,
						2, "100%");
			} else if (propertyId.equals("description")) {
				this.informationLayout.addComponent(field, "Description",
						0, 1, 2, "100%");
			} else if (propertyId.equals("environment")) {
				this.informationLayout.addComponent(field, "Environment",
						0, 2, 2, "100%");
			} else if (propertyId.equals("status")) {
				this.informationLayout.addComponent(field, "Status", 0, 3);
			} else if (propertyId.equals("priority")) {
				this.informationLayout
				.addComponent(field, "Priority", 1, 3);
			} else if (propertyId.equals("severity")) {
				this.informationLayout
				.addComponent(field, "Severity", 0, 4);
			} else if (propertyId.equals("resolution")) {
				this.informationLayout.addComponent(field, "Resolution", 1,
						4);
			} else if (propertyId.equals("duedate")) {
				this.informationLayout
				.addComponent(field, "Due Date", 0, 5);
			} else if (propertyId.equals("createdtime")) {
				this.informationLayout.addComponent(field, "Created Time",
						1, 5);
			} else if (propertyId.equals("loguserFullName")) {
				this.informationLayout.addComponent(field, "Logged by", 0,
						6);
			} else if (propertyId.equals("assignuserFullName")) {
				this.informationLayout
				.addComponent(
						field,
						LocalizationHelper
						.getMessage(GenericI18Enum.FORM_ASSIGNEE_FIELD),
						1, 6);
			} else if (propertyId.equals("milestoneName")) {
				this.informationLayout.addComponent(field,
						LocalizationHelper
						.getMessage(TaskI18nEnum.FORM_PHASE_FIELD),
						0, 7, 2, "100%");
			} else if (propertyId.equals("components")) {
				this.informationLayout.addComponent(field, "Components", 0,
						8, 2, "100%");
			} else if (propertyId.equals("affectedVersions")) {
				this.informationLayout.addComponent(field,
						"Affected Versions", 0, 9, 2, "100%");
			} else if (propertyId.equals("fixedVersions")) {
				this.informationLayout.addComponent(field,
						"Fixed Versions", 0, 10, 2, "100%");
			} else if (propertyId.equals("id")) {
				this.informationLayout.addComponent(field, "Attachments",
						0, 11, 2, "100%");
			} else {
				return false;
			}

			return true;
		}

		@Override
		public Layout getLayout() {
			this.informationLayout = new GridFormLayoutHelper(2, 12,
					"100%", "167px", Alignment.MIDDLE_LEFT);
			this.informationLayout.getLayout().addStyleName(
					"colored-gridlayout");
			this.informationLayout.getLayout().setMargin(false);
			this.informationLayout.getLayout().setWidth("100%");

			return this.informationLayout.getLayout();
		}
	}

	private class PreviewFormFieldFactory extends
	AbstractBeanFieldGroupViewFieldFactory<SimpleBug> {
		private static final long serialVersionUID = 1L;

		public PreviewFormFieldFactory(GenericBeanForm<SimpleBug> form) {
			super(form);
		}

		@Override
		protected Field<?> onCreateField(final Object propertyId) {
			if (propertyId.equals("duedate")) {
				return new FormDateViewField(
						beanItem.getDuedate());
			} else if (propertyId.equals("createdtime")) {
				return new FormDateViewField(
						beanItem.getCreatedtime());
			} else if (propertyId.equals("assignuserFullName")) {
				return new ProjectUserFormLinkField(
						beanItem.getAssignuser(),
						beanItem.getAssignUserAvatarId(),
						beanItem.getAssignuserFullName());
			} else if (propertyId.equals("loguserFullName")) {
				return new ProjectUserFormLinkField(
						beanItem.getLogby(),
						beanItem.getLoguserAvatarId(),
						beanItem.getLoguserFullName());
			} else if (propertyId.equals("id")) {
				return new ProjectFormAttachmentDisplayField(
						beanItem.getProjectid(),
						AttachmentType.PROJECT_BUG_TYPE,
						beanItem.getId());
			} else if (propertyId.equals("components")) {
				final List<Component> components = beanItem
						.getComponents();
				if (components != null && components.size() > 0) {
					final FormContainerViewField componentContainer = new FormContainerViewField();
					for (final Component component : beanItem
							.getComponents()) {
						final Button componentLink = new Button(
								component.getComponentname(),
								new Button.ClickListener() {
									private static final long serialVersionUID = 1L;

									@Override
									public void buttonClick(
											final ClickEvent event) {
										EventBus.getInstance()
										.fireEvent(
												new BugComponentEvent.GotoRead(
														BugReadViewImpl.this,
														component.getId()));
									}
								});
						componentContainer.addComponentField(componentLink);
						componentLink.setStyleName("link");
					}
					componentContainer
					.setStyleName(UIConstants.FORM_CONTAINER_VIEW);
					return componentContainer;
				} else {
					return new FormViewField("");
				}
			} else if (propertyId.equals("affectedVersions")) {
				final List<Version> affectedVersions = beanItem
						.getAffectedVersions();
				if (affectedVersions != null && affectedVersions.size() > 0) {
					final FormContainerViewField componentContainer = new FormContainerViewField();
					for (final Version version : beanItem
							.getAffectedVersions()) {
						final Button versionLink = new Button(
								version.getVersionname(),
								new Button.ClickListener() {
									private static final long serialVersionUID = 1L;

									@Override
									public void buttonClick(
											final ClickEvent event) {
										EventBus.getInstance()
										.fireEvent(
												new BugVersionEvent.GotoRead(
														BugReadViewImpl.this, version.getId()));
									}
								});
						componentContainer.addComponentField(versionLink);
						versionLink.setStyleName("link");
					}
					return componentContainer;
				} else {
					return new FormViewField("");
				}
			} else if (propertyId.equals("fixedVersions")) {
				final List<Version> fixedVersions = beanItem
						.getFixedVersions();
				if (fixedVersions != null && fixedVersions.size() > 0) {
					final FormContainerViewField componentContainer = new FormContainerViewField();
					for (final Version version : beanItem
							.getFixedVersions()) {
						final Button versionLink = new Button(
								version.getVersionname(),
								new Button.ClickListener() {
									private static final long serialVersionUID = 1L;

									@Override
									public void buttonClick(
											final ClickEvent event) {
										EventBus.getInstance()
										.fireEvent(
												new BugVersionEvent.GotoRead(
														BugReadViewImpl.this,
														version.getId()));
									}
								});
						componentContainer.addComponentField(versionLink);
						versionLink.setStyleName("link");
					}
					return componentContainer;
				} else {
					return new FormViewField("");
				}

			} else if (propertyId.equals("milestoneName")) {
				if (beanItem.getMilestoneid() != null) {
					final FormLinkViewField phaseLink = new FormLinkViewField(
							beanItem.getMilestoneName(),
							new Button.ClickListener() {
								private static final long serialVersionUID = 1L;

								@Override
								public void buttonClick(
										final ClickEvent event) {
									EventBus.getInstance()
									.fireEvent(
											new MilestoneEvent.GotoRead(
													BugReadViewImpl.this,
													beanItem
													.getMilestoneid()));
								}
							},
							MyCollabResource
							.newResource("icons/16/project/milestone.png"));
					return phaseLink;
				} else {
					return new FormViewField("");
				}

			} else if (propertyId.equals("environment")) {
				return new DefaultFormViewFieldFactory.FormDetectAndDisplayUrlViewField(
						beanItem.getEnvironment());
			} else if (propertyId.equals("description")) {
				return new DefaultFormViewFieldFactory.FormDetectAndDisplayUrlViewField(
						beanItem.getDescription());
			} else if (propertyId.equals("priority")) {
				if (StringUtils.isNotNullOrEmpty(beanItem
						.getPriority())) {
					final Resource iconPriority = ProjectResources
							.getIconResource12ByBugPriority(beanItem
									.getPriority());
					final Image iconEmbedded = new Image(null, iconPriority);
					final Label lbPriority = new Label(
							beanItem.getPriority());

					final FormContainerHorizontalViewField containerField = new FormContainerHorizontalViewField();
					containerField.addComponentField(iconEmbedded);
					containerField.addComponentField(lbPriority);
					containerField.getLayout().setExpandRatio(lbPriority,
							1.0f);
					return containerField;
				}
			} else if (propertyId.equals("severity")) {
				if (StringUtils.isNotNullOrEmpty(beanItem
						.getSeverity())) {
					final Resource iconPriority = ProjectResources
							.getIconResource12ByBugSeverity(beanItem
									.getSeverity());
					final Image iconEmbedded = new Image();
					iconEmbedded.setSource(iconPriority);
					final Label lbPriority = new Label(
							beanItem.getSeverity());

					final FormContainerHorizontalViewField containerField = new FormContainerHorizontalViewField();
					containerField.addComponentField(iconEmbedded);
					containerField.addComponentField(lbPriority);
					containerField.getLayout().setExpandRatio(lbPriority,
							1.0f);
					return containerField;
				}
			}
			return null;
		}
	}

	@Override
	public HasPreviewFormHandlers<SimpleBug> getPreviewFormHandlers() {
		return this.previewForm;
	}
}