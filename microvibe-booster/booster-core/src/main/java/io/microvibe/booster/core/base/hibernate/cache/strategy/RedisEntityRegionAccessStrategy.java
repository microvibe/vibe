package io.microvibe.booster.core.base.hibernate.cache.strategy;

import io.microvibe.booster.core.base.hibernate.cache.region.RedisEntityRegion;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

public class RedisEntityRegionAccessStrategy extends AbstractRegionAccessStrategy<RedisEntityRegion>
	implements EntityRegionAccessStrategy {

	public RedisEntityRegionAccessStrategy(RedisEntityRegion region, SessionFactoryOptions settings) {
		super(region, settings);
	}

	@Override
	public Object generateCacheKey(Object id, EntityPersister persister, SessionFactoryImplementor factory,
								   String tenantIdentifier) {
		return DefaultCacheKeysFactory.staticCreateEntityKey(id, persister, factory, tenantIdentifier);
	}

	@Override
	public Object getCacheKeyId(Object cacheKey) {
		return DefaultCacheKeysFactory.staticGetEntityId(cacheKey);
	}

	@Override
	public EntityRegion getRegion() {
		return region();
	}

	@Override
	public boolean insert(SessionImplementor session, Object key, Object value, Object version) throws CacheException {
		return false;
	}

	@Override
	public boolean afterInsert(SessionImplementor session, Object key, Object value, Object version)
		throws CacheException {
		return false;
	}

	@Override
	public boolean update(SessionImplementor session, Object key, Object value, Object currentVersion,
						  Object previousVersion) throws CacheException {
		remove(session, key);
		return false;
	}

	@Override
	public boolean afterUpdate(SessionImplementor session, Object key, Object value, Object currentVersion,
							   Object previousVersion, SoftLock lock) throws CacheException {
		unlockItem(session, key, lock);
		return false;
	}

}
