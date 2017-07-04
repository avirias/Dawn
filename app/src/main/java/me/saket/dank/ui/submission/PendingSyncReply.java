package me.saket.dank.ui.submission;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Contribution;

import rx.functions.Func1;

/**
 * See {@link CommentsManager}.
 */
@AutoValue
public abstract class PendingSyncReply {

  public static final String TABLE_NAME = "PendingSyncReply";
  private static final String COLUMN_PARENT_CONTRIBUTION_FULL_NAME = "parent_contribution_full_name";
  private static final String COLUMN_BODY = "body";
  private static final String COLUMN_STATE = "state";
  private static final String COLUMN_PARENT_SUBMISSION_FULL_NAME = "parent_submission_full_name";
  private static final String COLUMN_AUTHOR = "author";
  private static final String COLUMN_CREATED_TIME_MILLIS = "created_time_millis";
  private static final String COLUMN_POSTED_FULLNAME = "posted_fullname";

  public static final String QUERY_CREATE_TABLE =
      "CREATE TABLE " + TABLE_NAME + " ("
          + COLUMN_PARENT_CONTRIBUTION_FULL_NAME + " TEXT NOT NULL, "
          + COLUMN_BODY + " TEXT NOT NULL, "
          + COLUMN_STATE + " TEXT NOT NULL, "
          + COLUMN_PARENT_SUBMISSION_FULL_NAME + " TEXT NOT NULL, "
          + COLUMN_AUTHOR + " TEXT NOT NULL, "
          + COLUMN_CREATED_TIME_MILLIS + " INTEGER NOT NULL, "
          + COLUMN_POSTED_FULLNAME + " TEXT, "
          + "PRIMARY KEY (" + COLUMN_BODY + ", " + COLUMN_CREATED_TIME_MILLIS + ")"
          + ")";

  public static final String QUERY_GET_ALL_FOR_SUBMISSION =
      "SELECT * FROM " + TABLE_NAME
          + " WHERE " + COLUMN_PARENT_SUBMISSION_FULL_NAME + " == ?"
          + " ORDER BY " + COLUMN_CREATED_TIME_MILLIS + " DESC";

  public static final String QUERY_GET_ALL_POSTED_FOR_SUBMISSION =
      "SELECT * FROM " + TABLE_NAME
          + " WHERE " + COLUMN_PARENT_SUBMISSION_FULL_NAME + " == ?"
          + " AND " + COLUMN_STATE + " == '" + State.POSTED + "'"
          + " ORDER BY " + COLUMN_CREATED_TIME_MILLIS + " DESC";

  public static final String QUERY_GET_ALL_FAILED =
      "SELECT * FROM " + TABLE_NAME
          + " WHERE " + COLUMN_STATE + " == '" + State.FAILED + "'";

  public static final String WHERE_BODY_AND_CREATED_TIME_2 =
      COLUMN_BODY + " = ? AND " + COLUMN_CREATED_TIME_MILLIS + " = ?";

  public abstract String body();

  public abstract State state();

  public abstract String parentSubmissionFullName();

  /**
   * Full-name of parent {@link Contribution}.
   */
  public abstract String parentContributionFullName();

  public abstract String author();

  public abstract long createdTimeMillis();

  /**
   * Full-name of this comment on remote if it has been posted.
   */
  @Nullable
  public abstract String postedFullName();

  public enum State {
    POSTING,
    POSTED,
    FAILED,
  }

  public static PendingSyncReply create(String body, State state, String parentSubmissionFullName, String parentContributionFullName, String author,
      long createdTime)
  {
    return create(body, state, parentSubmissionFullName, parentContributionFullName, author, createdTime, null);
  }

  private static PendingSyncReply create(String body, State state, String parentSubmissionFullName, String parentContributionFullName, String author,
      long createdTime, @Nullable String postedFullName)
  {
    return new AutoValue_PendingSyncReply.Builder()
        .body(body)
        .state(state)
        .parentSubmissionFullName(parentSubmissionFullName)
        .parentContributionFullName(parentContributionFullName)
        .author(author)
        .createdTimeMillis(createdTime)
        .postedFullName(postedFullName)
        .build();
  }

  public PendingSyncReply.Builder toBuilder() {
    return new AutoValue_PendingSyncReply.Builder(this);
  }

  public ContentValues toValues() {
    ContentValues contentValues = new ContentValues(7);
    contentValues.put(COLUMN_PARENT_CONTRIBUTION_FULL_NAME, parentContributionFullName());
    contentValues.put(COLUMN_BODY, body());
    contentValues.put(COLUMN_STATE, state().name());
    contentValues.put(COLUMN_PARENT_SUBMISSION_FULL_NAME, parentSubmissionFullName());
    contentValues.put(COLUMN_AUTHOR, author());
    contentValues.put(COLUMN_CREATED_TIME_MILLIS, createdTimeMillis());
    contentValues.put(COLUMN_POSTED_FULLNAME, postedFullName());
    return contentValues;
  }

  public static final Func1<Cursor, PendingSyncReply> MAPPER = cursor -> {
    String parentContributionFullName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PARENT_CONTRIBUTION_FULL_NAME));
    String body = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BODY));
    State state = State.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATE)));
    String parentSubmissionFullName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PARENT_SUBMISSION_FULL_NAME));
    String author = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUTHOR));
    long createdTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_TIME_MILLIS));
    String postedFullName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POSTED_FULLNAME));
    return create(body, state, parentSubmissionFullName, parentContributionFullName, author, createdTime, postedFullName);
  };

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder parentContributionFullName(String parentContributionFullName);

    public abstract Builder body(String body);

    public abstract Builder state(State state);

    public abstract Builder parentSubmissionFullName(String parentSubmissionFullName);

    public abstract Builder author(String author);

    public abstract Builder createdTimeMillis(long createdTimeMillis);

    public abstract Builder postedFullName(String postedFullName);

    public abstract PendingSyncReply build();
  }
}
