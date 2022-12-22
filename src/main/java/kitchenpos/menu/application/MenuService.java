package kitchenpos.menu.application;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kitchenpos.common.Price;
import kitchenpos.exception.EntityNotFoundException;
import kitchenpos.exception.ErrorMessage;
import kitchenpos.menu.domain.Menu;
import kitchenpos.menu.domain.MenuProduct;
import kitchenpos.menu.domain.MenuProducts;
import kitchenpos.menu.domain.MenuRepository;
import kitchenpos.menu.dto.MenuRequest;
import kitchenpos.menu.dto.MenuResponse;
import kitchenpos.menugroup.domain.MenuGroup;
import kitchenpos.menugroup.repository.MenuGroupRepository;
import kitchenpos.product.domain.Product;
import kitchenpos.product.repository.ProductRepository;

@Service
public class MenuService {
	private final MenuRepository menuRepository;
	private final MenuGroupRepository menuGroupRepository;
	private final ProductRepository productRepository;

	public MenuService(
		MenuRepository menuRepository,
		MenuGroupRepository menuGroupRepository,
		ProductRepository productRepository
	) {
		this.menuRepository = menuRepository;
		this.menuGroupRepository = menuGroupRepository;
		this.productRepository = productRepository;
	}

	@Transactional
	public MenuResponse create(final MenuRequest menuRequest) {
		Menu menu = menuRequest.toEntity();
		validateMenuGroup(menu);
		validatePrice(menu);
		return MenuResponse.of(menuRepository.save(menu));
	}

	public List<MenuResponse> list() {
		return menuRepository.findAll()
			.stream()
			.map(MenuResponse::of)
			.collect(Collectors.toList());
	}

	private void validateMenuGroup(Menu menu) {
		if (!menuGroupRepository.existsById(menu.getMenuGroupId())) {
			throw new EntityNotFoundException(MenuGroup.ENTITY_NAME, menu.getMenuGroupId());
		}
	}

	private void validatePrice(Menu menu) {
		Price menuProductsPrice = getTotalPrice(menu.getMenuProducts());
		if (menu.moreExpensive(menuProductsPrice)) {
			throw new IllegalArgumentException(ErrorMessage.PRICE_HIGHER_THAN_MENU_PRODUCTS_TOTAL_PRICES);
		}
	}

	private Price getTotalPrice(MenuProducts menuProducts) {
		return menuProducts.value()
			.stream()
			.map(it -> productPrice(it.getProductId(), it))
			.reduce(Price::add)
			.orElse(Price.ZERO);
	}

	private Price productPrice(Long productId, MenuProduct menuProduct) {
		Price price = productRepository.findById(productId)
			.orElseThrow(() -> new EntityNotFoundException(Product.ENTITY_NAME, productId))
			.getPrice();
		return menuProduct.getTotalPrice(price);
	}
}
