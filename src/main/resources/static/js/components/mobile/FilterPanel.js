const { createApp, ref } = Vue;

createApp({
    setup() {
        const data = window.__BLOG_DATA__ || {};
        const categories = data.categories || [];
        const currentCategory = ref(data.selectedCategory || 'latest');
        const sortBy = ref(data.sortBy || 'newest');
        const isOpen = ref(false);

        const sortOptions = [
            { value: 'newest', label: '最新' },
            { value: 'popular', label: '热门' },
            { value: 'featured', label: '精选' },
            { value: 'pinned', label: '置顶' },
        ];

        function toggle() { isOpen.value = !isOpen.value; }
        function close() { isOpen.value = false; }

        function selectCategory(slug) {
            currentCategory.value = slug;
            isOpen.value = false;
            const params = new URLSearchParams();
            if (slug !== 'latest') params.set('category', slug);
            if (sortBy.value !== 'newest') params.set('sort', sortBy.value);
            window.location.href = '/posts?' + params.toString();
        }

        function selectSort(val) {
            sortBy.value = val;
            selectCategory(currentCategory.value);
        }

        return { categories, currentCategory, sortBy, sortOptions, isOpen, toggle, close, selectCategory, selectSort };
    },
    template: `
        <div>
            <button class="ll-bottom-nav-item" @click="toggle" aria-label="筛选">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="4" y1="6" x2="20" y2="6"/><line x1="8" y1="12" x2="16" y2="12"/>
                    <line x1="11" y1="18" x2="13" y2="18"/>
                </svg>
                <span>筛选</span>
            </button>
            <div class="ll-filter-drawer" :class="{ 'is-open': isOpen }">
                <div class="ll-filter-drawer-title">择文而读</div>
                <div class="ll-filter-chip-row">
                    <button class="ll-filter-chip" :class="{ 'is-active': currentCategory === 'latest' }"
                            @click="selectCategory('latest')">全部</button>
                    <button v-for="cat in categories" :key="cat.slug"
                            class="ll-filter-chip" :class="{ 'is-active': currentCategory === cat.slug }"
                            @click="selectCategory(cat.slug)">{{ cat.displayName }}</button>
                </div>
                <div class="ll-filter-drawer-title" style="margin-top:14px">排序方式</div>
                <div class="ll-filter-chip-row">
                    <button v-for="opt in sortOptions" :key="opt.value"
                            class="ll-filter-chip" :class="{ 'is-active': sortBy === opt.value }"
                            @click="selectSort(opt.value)">{{ opt.label }}</button>
                </div>
            </div>
            <div v-if="isOpen" style="position:fixed;inset:0;z-index:98" @click="close"></div>
        </div>
    `
}).mount('#mobile-filter-app');
