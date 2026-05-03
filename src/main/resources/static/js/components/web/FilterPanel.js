const { createApp, ref, computed } = Vue;

createApp({
    setup() {
        const data = window.__BLOG_DATA__ || {};
        const categories = data.categories || [];
        const currentCategory = ref(data.selectedCategory || 'latest');
        const sortBy = ref(data.sortBy || 'newest');

        const sortOptions = [
            { value: 'newest', label: '最新发布' },
            { value: 'popular', label: '最多点赞' },
            { value: 'featured', label: '精选优先' },
            { value: 'pinned', label: '置顶优先' },
        ];

        function navigate() {
            const params = new URLSearchParams();
            if (currentCategory.value !== 'latest') {
                params.set('category', currentCategory.value);
            }
            if (sortBy.value !== 'newest') {
                params.set('sort', sortBy.value);
            }
            window.location.href = '/posts?' + params.toString();
        }

        function selectCategory(slug) {
            currentCategory.value = slug;
            navigate();
        }

        function selectSort(val) {
            sortBy.value = val;
            navigate();
        }

        return { categories, currentCategory, sortBy, sortOptions, selectCategory, selectSort };
    },
    template: `
        <div class="ll-filter-panel">
            <div class="ll-filter-section">
                <div class="ll-filter-label">择文而读</div>
                <div class="ll-filter-chip-row">
                    <button class="ll-filter-chip" :class="{ 'is-active': currentCategory === 'latest' }"
                            @click="selectCategory('latest')">全部</button>
                    <button v-for="cat in categories" :key="cat.slug"
                            class="ll-filter-chip" :class="{ 'is-active': currentCategory === cat.slug }"
                            @click="selectCategory(cat.slug)">{{ cat.displayName }}</button>
                </div>
            </div>
            <div class="ll-filter-section" style="margin-top:16px">
                <div class="ll-filter-label">排序方式</div>
                <div class="ll-filter-chip-row">
                    <button v-for="opt in sortOptions" :key="opt.value"
                            class="ll-filter-chip" :class="{ 'is-active': sortBy === opt.value }"
                            @click="selectSort(opt.value)">{{ opt.label }}</button>
                </div>
            </div>
        </div>
    `
}).mount('#filter-panel-app');
