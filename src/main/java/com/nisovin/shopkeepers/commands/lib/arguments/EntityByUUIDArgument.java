package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;

/**
 * Determines an existing entity by the given UUID input.
 */
public class EntityByUUIDArgument extends ObjectByIdArgument<UUID, Entity> {

	public EntityByUUIDArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public EntityByUUIDArgument(String name, ArgumentFilter<Entity> filter) {
		this(name, filter, EntityUUIDArgument.DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public EntityByUUIDArgument(String name, ArgumentFilter<Entity> filter, int minimalCompletionInput) {
		super(name, filter, minimalCompletionInput);
	}

	@Override
	protected ObjectIdArgument<UUID> createIdArgument(String name, int minimalCompletionInput) {
		return new EntityUUIDArgument(name, ArgumentFilter.acceptAny(), minimalCompletionInput) {
			@Override
			protected Iterable<UUID> getCompletionSuggestions(String idPrefix) {
				return EntityByUUIDArgument.this.getCompletionSuggestions(idPrefix);
			}
		};
	}

	@Override
	protected Entity getObject(UUID uuid) throws ArgumentParseException {
		return Bukkit.getEntity(uuid);
	}

	@Override
	protected Iterable<UUID> getCompletionSuggestions(String idPrefix) {
		return EntityUUIDArgument.getDefaultCompletionSuggestions(idPrefix, filter);
	}
}
