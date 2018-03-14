package me.saket.dank.ui.subreddit;

import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import com.jakewharton.rxrelay2.PublishRelay;

import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import javax.inject.Inject;

import dagger.Lazy;
import me.saket.dank.R;
import me.saket.dank.data.OnLoginRequireListener;
import me.saket.dank.data.VotingManager;
import me.saket.dank.ui.submission.BookmarksRepository;
import me.saket.dank.ui.submission.events.ContributionVoteSwipeEvent;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.utils.Animations;
import me.saket.dank.widgets.swipe.SwipeAction;
import me.saket.dank.widgets.swipe.SwipeActionIconView;
import me.saket.dank.widgets.swipe.SwipeActions;
import me.saket.dank.widgets.swipe.SwipeActionsHolder;
import me.saket.dank.widgets.swipe.SwipeTriggerRippleDrawable.RippleType;
import me.saket.dank.widgets.swipe.SwipeableLayout;

/**
 * Controls gesture actions on submissions.
 */
public class SubmissionSwipeActionsProvider implements SwipeableLayout.SwipeActionIconProvider {

  private static final @StringRes int ACTION_NAME_SAVE = R.string.subreddit_submission_swipe_action_save;
  private static final @StringRes int ACTION_NAME_UNSAVE = R.string.subreddit_submission_swipe_action_unsave;
  private static final @StringRes int ACTION_NAME_OPTIONS = R.string.subreddit_submission_swipe_action_options;
  private static final @StringRes int ACTION_NAME_UPVOTE = R.string.subreddit_submission_swipe_action_upvote;
  private static final @StringRes int ACTION_NAME_DOWNVOTE = R.string.subreddit_submission_swipe_action_downvote;

  private final Lazy<VotingManager> votingManager;
  private final Lazy<BookmarksRepository> bookmarksRepository;
  private final Lazy<UserSessionRepository> userSessionRepository;
  private final Lazy<OnLoginRequireListener> onLoginRequireListener;
  private final SwipeActions swipeActionsWithUnSave;
  private final SwipeActions swipeActionsWithSave;
  public final PublishRelay<SubmissionOptionSwipeEvent> optionSwipeActions = PublishRelay.create();
  public final PublishRelay<ContributionVoteSwipeEvent> voteSwipeActions = PublishRelay.create();

  @Inject
  public SubmissionSwipeActionsProvider(
      Lazy<VotingManager> votingManager,
      Lazy<BookmarksRepository> bookmarksRepository,
      Lazy<UserSessionRepository> userSessionRepository,
      Lazy<OnLoginRequireListener> onLoginRequireListener)
  {
    this.votingManager = votingManager;
    this.bookmarksRepository = bookmarksRepository;
    this.userSessionRepository = userSessionRepository;
    this.onLoginRequireListener = onLoginRequireListener;

    SwipeAction moreOptionsSwipeAction = SwipeAction.create(ACTION_NAME_OPTIONS, R.color.list_item_swipe_more_options, 0.3f);
    SwipeAction saveSwipeAction = SwipeAction.create(ACTION_NAME_SAVE, R.color.list_item_swipe_save, 0.7f);
    SwipeAction unSaveSwipeAction = SwipeAction.create(ACTION_NAME_UNSAVE, R.color.list_item_swipe_save, 0.7f);
    SwipeAction downvoteSwipeAction = SwipeAction.create(ACTION_NAME_DOWNVOTE, R.color.list_item_swipe_downvote, 0.7f);
    SwipeAction upvoteSwipeAction = SwipeAction.create(ACTION_NAME_UPVOTE, R.color.list_item_swipe_upvote, 0.3f);

    // Actions on both sides are aligned from left to right.
    swipeActionsWithUnSave = SwipeActions.builder()
        .startActions(SwipeActionsHolder.builder()
            .add(moreOptionsSwipeAction)
            .add(unSaveSwipeAction)
            .build())
        .endActions(SwipeActionsHolder.builder()
            .add(downvoteSwipeAction)
            .add(upvoteSwipeAction)
            .build())
        .build();

    swipeActionsWithSave = SwipeActions.builder()
        .startActions(SwipeActionsHolder.builder()
            .add(moreOptionsSwipeAction)
            .add(saveSwipeAction)
            .build())
        .endActions(SwipeActionsHolder.builder()
            .add(upvoteSwipeAction)
            .add(downvoteSwipeAction)
            .build())
        .build();
  }

  public SwipeActions actionsFor(Submission submission) {
    boolean isSubmissionSaved = bookmarksRepository.get().isSaved(submission);
    return isSubmissionSaved ? swipeActionsWithUnSave : swipeActionsWithSave;
  }

  @Override
  public void showSwipeActionIcon(SwipeActionIconView imageView, @Nullable SwipeAction oldAction, SwipeAction newAction) {
    switch (newAction.labelRes()) {
      case ACTION_NAME_OPTIONS:
        resetIconRotation(imageView);
        imageView.setImageResource(R.drawable.ic_more_horiz_24dp);
        break;

      case ACTION_NAME_SAVE:
        resetIconRotation(imageView);
        imageView.setImageResource(R.drawable.ic_save_24dp);
        break;

      case ACTION_NAME_UNSAVE:
        resetIconRotation(imageView);
        imageView.setImageResource(R.drawable.ic_unsave_24dp);
        break;

      case ACTION_NAME_UPVOTE:
        if (oldAction != null && ACTION_NAME_DOWNVOTE == oldAction.labelRes()) {
          imageView.setRotation(180);   // We want to play a circular animation if the user keeps switching between upvote and downvote.
          imageView.animate().rotationBy(180).setInterpolator(Animations.INTERPOLATOR).setDuration(200).start();
        } else {
          resetIconRotation(imageView);
          imageView.setImageResource(R.drawable.ic_arrow_upward_24dp);
        }
        break;

      case ACTION_NAME_DOWNVOTE:
        if (oldAction != null && ACTION_NAME_UPVOTE == oldAction.labelRes()) {
          imageView.setRotation(0);
          imageView.animate().rotationBy(180).setInterpolator(Animations.INTERPOLATOR).setDuration(200).start();
        } else {
          resetIconRotation(imageView);
          imageView.setImageResource(R.drawable.ic_arrow_downward_24dp);
        }
        break;

      default:
        throw new UnsupportedOperationException("Unknown swipe action: " + newAction);
    }
  }

  public void performSwipeAction(SwipeAction swipeAction, Submission submission, SwipeableLayout swipeableLayout) {
    if (ACTION_NAME_OPTIONS != swipeAction.labelRes() && !userSessionRepository.get().isUserLoggedIn()) {
      onLoginRequireListener.get().onLoginRequired();
      return;
    }

    boolean isUndoAction;

    switch (swipeAction.labelRes()) {
      case ACTION_NAME_OPTIONS:
        optionSwipeActions.accept(SubmissionOptionSwipeEvent.create(submission, swipeableLayout));
        isUndoAction = false;
        break;

      case ACTION_NAME_SAVE:
        bookmarksRepository.get().markAsSaved(submission);
        isUndoAction = false;
        break;

      case ACTION_NAME_UNSAVE:
        bookmarksRepository.get().markAsUnsaved(submission);
        isUndoAction = true;
        break;

      case ACTION_NAME_UPVOTE: {
        VoteDirection currentVoteDirection = votingManager.get().getPendingOrDefaultVote(submission, submission.getVote());
        VoteDirection newVoteDirection = currentVoteDirection == VoteDirection.UPVOTE ? VoteDirection.NO_VOTE : VoteDirection.UPVOTE;
        voteSwipeActions.accept(ContributionVoteSwipeEvent.create(submission, newVoteDirection));
        isUndoAction = newVoteDirection == VoteDirection.NO_VOTE;
        break;
      }

      case ACTION_NAME_DOWNVOTE: {
        VoteDirection currentVoteDirection = votingManager.get().getPendingOrDefaultVote(submission, submission.getVote());
        VoteDirection newVoteDirection = currentVoteDirection == VoteDirection.DOWNVOTE ? VoteDirection.NO_VOTE : VoteDirection.DOWNVOTE;
        voteSwipeActions.accept(ContributionVoteSwipeEvent.create(submission, newVoteDirection));
        isUndoAction = newVoteDirection == VoteDirection.NO_VOTE;
        break;
      }

      default:
        throw new UnsupportedOperationException("Unknown swipe action: " + swipeAction);
    }

    swipeableLayout.playRippleAnimation(swipeAction, isUndoAction ? RippleType.UNDO : RippleType.REGISTER);
  }

  private void resetIconRotation(SwipeActionIconView imageView) {
    imageView.animate().cancel();
    imageView.setRotation(0);
  }
}
