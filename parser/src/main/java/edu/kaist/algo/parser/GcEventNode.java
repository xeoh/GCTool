/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.
 * ----------------------------------------------------------------------------
 */

package edu.kaist.algo.parser;

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
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

  @Nullable public abstract Double cmsCpuTime();

  @Nullable public abstract Double cmsWallTime();

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

    public abstract Builder cmsCpuTime(Double cpuTime);

    public abstract Builder cmsWallTime(Double wallTime);

    abstract ImmutableList.Builder<GcEventNode> childrenBuilder();

    public Builder addChild(GcEventNode child) {
      childrenBuilder().add(child);
      return this;
    }

    public abstract GcEventNode build();
  }

  public String typeAndDetail() {
    final StringBuilder sb = new StringBuilder(type());
    if (!Strings.isNullOrEmpty(detail())) {
      sb.append(" (").append(detail()).append(")");
    }
    return sb.toString();
  }
}
