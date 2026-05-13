package com.openroof.openroof.screening.adapter;

import com.openroof.openroof.model.enums.ScreeningProvider;
import com.openroof.openroof.model.rental.RentalApplication;

public interface ScreeningProviderAdapter {

    ScreeningProvider provider();

    ScreeningResult runScreening(RentalApplication application);
}
