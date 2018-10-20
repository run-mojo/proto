package run.mojo.wire;

import com.google.common.collect.ImmutableMap;

/**
 *
 */
public class AbstractPackage {
    public String name;
    public ImmutableMap<String, FunctionDescriptor<?, ?>> actions;
    public ImmutableMap<String, MessageDescriptor<?>> messages;

    public ImmutableMap<String, AbstractPackage> packages;
}
