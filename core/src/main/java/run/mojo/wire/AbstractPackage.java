package run.mojo.wire;

import com.google.common.collect.ImmutableMap;
import run.mojo.model.MessageDescriptor;

/**
 *
 */
public class AbstractPackage {
    public String name;
    public ImmutableMap<String, FunctionDescriptor<?, ?>> actions;
    public ImmutableMap<String, MessageDescriptor<?>> messages;

    public ImmutableMap<String, AbstractPackage> packages;
}
