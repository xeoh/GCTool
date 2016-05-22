package edu.kaist.algo.parser;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;

@AutoValue
public abstract class GcEventNode {

  @Nullable public abstract Long timestamp();

  public abstract String type();

  @Nullable public abstract String detail();

  @Nullable public abstract Long prevUsage();

  @Nullable public abstract Long afterUsage();

  @Nullable public abstract Long capacity();

  @Nullable public abstract Double elapsedTime();

  @Nullable public abstract Double user();

  @Nullable public abstract Double sys();

  @Nullable public abstract Double real();

  public abstract ImmutableList<GcEventNode> children();

  public static Builder builder() {
    return new AutoValue_GcEventNode.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder timestamp(Long timestamp);

    public abstract Builder type(String type);

    public abstract Builder detail(String detail);

    public abstract Builder prevUsage(Long prevUsage);

    public abstract Builder afterUsage(Long afterUsage);

    public abstract Builder capacity(Long capacity);

    public abstract Builder elapsedTime(Double elapsedTime);

    public abstract Builder user(Double user);

    public abstract Builder sys(Double sys);

    public abstract Builder real(Double real);

    abstract ImmutableList.Builder<GcEventNode> childrenBuilder();

    public Builder addChild(GcEventNode child) {
      childrenBuilder().add(child);
      return this;
    }

    public abstract GcEventNode build();
  }
}
